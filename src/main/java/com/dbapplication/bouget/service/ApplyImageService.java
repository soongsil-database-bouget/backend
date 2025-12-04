package com.dbapplication.bouget.service;

import com.dbapplication.bouget.dto.ApplyImageResponse;
import com.dbapplication.bouget.dto.BouquetCategoryResponse;
import com.dbapplication.bouget.dto.BouquetResponse;
import com.dbapplication.bouget.entity.ApplyImage;
import com.dbapplication.bouget.entity.Bouquet;
import com.dbapplication.bouget.entity.BouquetCategory;
import com.dbapplication.bouget.entity.RecommendationSession;
import com.dbapplication.bouget.entity.User;
import com.dbapplication.bouget.entity.enums.ApplyStatus;
import com.dbapplication.bouget.repository.ApplyImageRepository;
import com.dbapplication.bouget.repository.BouquetCategoryRepository;
import com.dbapplication.bouget.repository.BouquetRepository;
import com.dbapplication.bouget.repository.RecommendationSessionRepository;
import com.dbapplication.bouget.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplyImageService {

    private final UserRepository userRepository;
    private final BouquetRepository bouquetRepository;
    private final RecommendationSessionRepository sessionRepository;
    private final ApplyImageRepository applyImageRepository;
    private final BouquetCategoryRepository bouquetCategoryRepository;
    private final WebClient fastapiWebClient;

    @Value("${file.upload-dir}")
    private String uploadDir;

    // ★ 추가: 서버 베이스 URL (예: http://52.78.57.66:8080)
    @Value("${app.server-base-url}")
    private String serverBaseUrl;

    /**
     * 이미지 적용 생성 플로우
     * 1) 유저 / 부케 / 세션 조회
     * 2) 유저 이미지 서버에 저장 → srcImageUrl
     * 3) 부케 이미지 바이트 가져오기
     * 4) FastAPI /api/composite-bouquet 호출 (multipart)
     * 5) FastAPI result_image_url 로 결과 이미지 다운로드 → 우리 서버에 저장 → genImageUrl
     * 6) ApplyImage 엔티티 저장
     */
    @Transactional
    public ApplyImageResponse createApplyImage(
            Long userId,
            Long bouquetId,
            Long sessionId,
            MultipartFile userImageFile
    ) {
        // 1. 엔티티 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found. id=" + userId));

        Bouquet bouquet = bouquetRepository.findById(bouquetId)
                .orElseThrow(() -> new IllegalArgumentException("Bouquet not found. id=" + bouquetId));

        RecommendationSession session = null;
        if (sessionId != null) {
            session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("RecommendationSession not found. id=" + sessionId));
        }

        try {
            // 2. 유저 원본 이미지 서버에 저장 → srcImageUrl
            String srcImageUrl = saveUserSrcImage(userImageFile);   // "/images/apply/src/xxxx.png"

            // 3. ApplyImage 엔티티를 PENDING 상태로 먼저 저장 (genImageUrl은 아직 없음)
            ApplyImage applyImage = ApplyImage.builder()
                    .user(user)
                    .bouquet(bouquet)
                    .session(session)
                    .srcImageUrl(srcImageUrl)
                    .genImageUrl("")               // NOT NULL 컬럼이라 빈 문자열
                    .status(ApplyStatus.PENDING)   // 상태 PENDING으로 시작
                    .build();

            ApplyImage saved = applyImageRepository.save(applyImage);

            // 4. 비동기로 합성 작업 시작 (MultipartFile 그대로 넘김)
            startCompositeAsync(saved.getId(), bouquet.getId(), userImageFile);

            // 5. 바로 응답 (status = PENDING, genImageUrl = null로 내려감)
            return toResponse(saved);

        } catch (IOException e) {
            log.error("createApplyImage IOException", e);
            throw new RuntimeException("이미지 처리 중 오류가 발생했습니다.", e);
        }
    }
    private void startCompositeAsync(Long applyImageId, Long bouquetId, MultipartFile userImageFile) {
        CompletableFuture.runAsync(() -> {
            try {
                log.info("Async composite start. applyImageId={}, bouquetId={}", applyImageId, bouquetId);

                // 1) ApplyImage, Bouquet 다시 조회 (repository가 자체 @Transactional 달고 있음)
                ApplyImage applyImage = applyImageRepository.findById(applyImageId)
                        .orElseThrow(() -> new IllegalArgumentException("ApplyImage not found. id=" + applyImageId));

                Bouquet bouquet = bouquetRepository.findById(bouquetId)
                        .orElseThrow(() -> new IllegalArgumentException("Bouquet not found. id=" + bouquetId));

                // 2) 부케 이미지 바이트 로딩 (기존 메서드 그대로 사용)
                byte[] bouquetBytes = loadBouquetImageBytes(bouquet);

                // 3) FastAPI 호출 (시그니처 그대로: MultipartFile + byte[])
                String resultImageUrlFromFastApi = callFastApiComposite(userImageFile, bouquetBytes);

                // 4) 결과 이미지 다운로드 → 우리 서버에 저장
                String genImageUrl = downloadAndSaveGeneratedImage(resultImageUrlFromFastApi);

                // 5) 상태 DONE + genImageUrl 업데이트
                applyImage.markDone(genImageUrl);
                applyImageRepository.save(applyImage);

                log.info("Async composite done. applyImageId={}, genImageUrl={}", applyImageId, genImageUrl);

            } catch (Exception e) {
                log.error("Async composite failed. applyImageId=" + applyImageId, e);
                // 실패 시 상태 FAILED만 찍어 둔다
                try {
                    applyImageRepository.findById(applyImageId).ifPresent(ai -> {
                        ai.markFailed();
                        applyImageRepository.save(ai);
                    });
                } catch (Exception ex) {
                    log.error("Failed to mark ApplyImage as FAILED. id=" + applyImageId, ex);
                }
            }
        });
    }

    @Transactional(readOnly = true)
    public Page<ApplyImageResponse> getApplyImagesByUser(Long userId, Pageable pageable) {
        // 유저 검증 (존재하지 않는 유저 방지)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found. id=" + userId));

        // 유저의 ApplyImage 페이지 조회
        Page<ApplyImage> page = applyImageRepository.findByUser(user, pageable);

        // 엔티티 → 응답 DTO 로 매핑
        return page.map(this::toResponse);
    }

    /**
     * ApplyImage 단건 조회
     */
    @Transactional(readOnly = true)
    public ApplyImageResponse getApplyImage(Long id) {
        ApplyImage applyImage = applyImageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ApplyImage not found. id=" + id));
        return toResponse(applyImage);
    }

    // === 내부 메서드들 ===

    private String saveUserSrcImage(MultipartFile userImageFile) throws IOException {
        String originalFilename = userImageFile.getOriginalFilename();
        String ext = getExtensionSafe(originalFilename);
        String filename = UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);

        Path savePath = Paths.get(uploadDir, "apply", "src", filename);
        Files.createDirectories(savePath.getParent());
        Files.copy(userImageFile.getInputStream(), savePath, StandardCopyOption.REPLACE_EXISTING);

        // 업로드 폴더 기준 상대 경로를 /images/** URL 로 반환 (DB에는 이 값 저장)
        String relativePath = "apply/src/" + filename;
        return "/images/" + relativePath.replace("\\", "/");
    }

    private byte[] loadBouquetImageBytes(Bouquet bouquet) throws IOException {
        String bouquetImageUrl = bouquet.getImageUrl();

        if (bouquetImageUrl == null || bouquetImageUrl.isBlank()) {
            throw new IllegalStateException("Bouquet image URL is empty. bouquetId=" + bouquet.getId());
        }

        // ★ 여기서 '/images/...' 또는 'http://.../images/...' 둘 다 처리
        String pathPart = bouquetImageUrl;

        // 절대 URL로 저장되어 있다면 '/images/...' 부분만 추출
        if (pathPart.startsWith("http://") || pathPart.startsWith("https://")) {
            int idx = pathPart.indexOf("/images/");
            if (idx == -1) {
                throw new IllegalStateException("Bouquet image URL must contain /images/. url=" + bouquetImageUrl);
            }
            pathPart = pathPart.substring(idx); // "/images/..."
        }

        if (!pathPart.startsWith("/images/")) {
            throw new IllegalStateException("Bouquet image URL must start with /images/. url=" + bouquetImageUrl);
        }

        String relative = pathPart.substring("/images/".length()); // "bouquets/bouquet000.png"
        Path path = Paths.get(uploadDir, relative);
        return Files.readAllBytes(path);
    }
    private MediaType resolveImageMediaType(String filename, String contentType) {
        if (contentType != null && !contentType.isBlank()) {
            return MediaType.parseMediaType(contentType);
        }

        if (filename != null) {
            String lower = filename.toLowerCase();
            if (lower.endsWith(".png")) {
                return MediaType.IMAGE_PNG;
            } else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
                return MediaType.IMAGE_JPEG;
            }
        }

        // 그래도 모르겠으면 PNG 하나로 통일해도 됨
        return MediaType.IMAGE_PNG;
    }

    private String callFastApiComposite(MultipartFile userImageFile, byte[] bouquetImageBytes) {
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();

        // user_image
        String userFilename = userImageFile.getOriginalFilename();
        MediaType userMediaType = resolveImageMediaType(userFilename, userImageFile.getContentType());

        bodyBuilder.part("user_image", userImageFile.getResource())
                .filename(userFilename)
                .contentType(userMediaType);

        // bouquet_image
        ByteArrayResource bouquetResource = new ByteArrayResource(bouquetImageBytes) {
            @Override
            public String getFilename() {
                return "bouquet.png";
            }
        };

        bodyBuilder.part("bouquet_image", bouquetResource)
                .filename("bouquet.png")
                .contentType(MediaType.IMAGE_PNG);  // PNG로 고정

        FastApiCompositeResponse fastRes = fastapiWebClient.post()
                .uri("/api/composite-bouquet")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.bodyToMono(FastApiCompositeResponse.class);
                    } else {
                        return response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(errorBody -> {
                                    log.error("[FastAPI ERROR] status={}, body={}",
                                            response.statusCode(), errorBody);
                                    return reactor.core.publisher.Mono.error(
                                            new RuntimeException("FastAPI error: " + response.statusCode()
                                                    + " body=" + errorBody)
                                    );
                                });
                    }
                })
                .block();

        if (fastRes == null || fastRes.result_image_url() == null || fastRes.result_image_url().isBlank()) {
            throw new IllegalStateException("FastAPI result_image_url is null or empty");
        }

        log.info("FastAPI composite result url = {}", fastRes.result_image_url());
        return fastRes.result_image_url();
    }


    private String downloadAndSaveGeneratedImage(String resultImageUrl) throws IOException {
        String filename = UUID.randomUUID() + ".png";
        Path savePath = Paths.get(uploadDir, "apply", "gen", filename);
        Files.createDirectories(savePath.getParent());

        try (InputStream in = new URL(resultImageUrl).openStream()) {
            Files.copy(in, savePath, StandardCopyOption.REPLACE_EXISTING);
        }

        // 업로드 폴더 기준 상대 경로를 /images/** URL 로 반환 (DB에는 이 값 저장)
        String relativePath = "apply/gen/" + filename;
        return "/images/" + relativePath.replace("\\", "/");
    }

    /**
     * ApplyImage -> ApplyImageResponse 매핑
     * - BouquetResponse (카테고리 포함) 함께 내려줌
     * - 이미지 URL은 클라이언트에서 바로 쓸 수 있게 풀 URL로 변환
     */
    private ApplyImageResponse toResponse(ApplyImage entity) {
        Bouquet bouquet = entity.getBouquet();

        // 부케 + 카테고리 DTO 생성
        BouquetResponse bouquetResponse = toBouquetResponse(bouquet);

        String srcImageFullUrl = buildFullImageUrl(entity.getSrcImageUrl());
        String genImageFullUrl = buildFullImageUrl(entity.getGenImageUrl());

        return new ApplyImageResponse(
                entity.getId(),
                entity.getUser().getId(),
                bouquet.getId(),
                entity.getSession() != null ? entity.getSession().getId() : null,
                srcImageFullUrl,
                genImageFullUrl,
                entity.getStatus(),
                entity.getCreatedAt(),
                bouquetResponse
        );
    }

    private BouquetResponse toBouquetResponse(Bouquet bouquet) {
        BouquetCategory categories = bouquetCategoryRepository.findByBouquet(bouquet);
        BouquetCategoryResponse categoryResponses = toCategoryResponse(categories);

        // ★ BouquetResponse에도 풀 URL로 세팅
        String bouquetImageFullUrl = buildFullImageUrl(bouquet.getImageUrl());

        return BouquetResponse.builder()
                .id(bouquet.getId())
                .name(bouquet.getName())
                .price(bouquet.getPrice())
                .reason(bouquet.getReason())
                .description(bouquet.getDescription())
                .imageUrl(bouquetImageFullUrl)
                .categories(categoryResponses)
                .build();
    }

    private BouquetCategoryResponse toCategoryResponse(BouquetCategory category) {
        return BouquetCategoryResponse.builder()
                .id(category.getId())
                .bouquetId(category.getBouquet().getId())
                .season(category.getSeason())
                .dressMood(category.getDressMood())
                .dressSilhouette(category.getDressSilhouette())
                .weddingColor(category.getWeddingColor())
                .bouquetAtmosphere(category.getBouquetAtmosphere())
                .usage(category.getUsage())
                .build();
    }

    private String getExtensionSafe(String filename) {
        if (filename == null) return "";
        int idx = filename.lastIndexOf('.');
        if (idx == -1 || idx == filename.length() - 1) {
            return "";
        }
        return filename.substring(idx + 1);
    }

    // ★ 공통: /images/... 또는 http... 다 받아서 풀 URL로 바꿔주는 함수
    private String buildFullImageUrl(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }

        // 이미 절대 URL이면 그대로 사용
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }

        String resultPath = path;
        if (!resultPath.startsWith("/")) {
            resultPath = "/" + resultPath;
        }

        return serverBaseUrl + resultPath;
    }

    private record FastApiCompositeResponse(
            String status,
            String original_user_file,
            String original_bouquet_file,
            String result_image_url
    ) {}
}
