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

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplyImageService {

    private final UserRepository userRepository;
    private final BouquetRepository bouquetRepository;
    private final RecommendationSessionRepository sessionRepository;
    private final ApplyImageRepository applyImageRepository;
    private final BouquetCategoryRepository bouquetCategoryRepository; // ✅ 추가
    private final WebClient fastapiWebClient;   // WebClientConfig 에서 fastapi용으로 등록한 Bean

    @Value("${file.upload-dir}")
    private String uploadDir;

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
            // 2. 유저 원본 이미지 서버에 저장
            String srcImageUrl = saveUserSrcImage(userImageFile);

            // 3. 부케 이미지 바이트 가져오기
            byte[] bouquetBytes = loadBouquetImageBytes(bouquet);

            // 4. FastAPI 호출해서 합성 이미지 URL 받기
            String resultImageUrlFromFastApi = callFastApiComposite(userImageFile, bouquetBytes);

            // 5. 결과 이미지 다운로드해서 우리 서버에 저장
            String genImageUrl = downloadAndSaveGeneratedImage(resultImageUrlFromFastApi);

            // 6. ApplyImage 엔티티 저장
            ApplyImage applyImage = ApplyImage.builder()
                    .user(user)
                    .bouquet(bouquet)
                    .session(session)
                    .srcImageUrl(srcImageUrl)
                    .genImageUrl(genImageUrl)
                    .status(ApplyStatus.DONE)
                    .build();

            ApplyImage saved = applyImageRepository.save(applyImage);

            return toResponse(saved);
        } catch (IOException e) {
            log.error("createApplyImage IOException", e);
            throw new RuntimeException("이미지 처리 중 오류가 발생했습니다.", e);
        }
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

        // ★ 업로드 폴더 기준 상대 경로를 /images/** URL 로 반환
        String relativePath = "apply/src/" + filename;           // 업로드 디렉터리 기준
        return "/images/" + relativePath.replace("\\", "/");      // 윈도우 대비 슬래시 통일
    }

    private byte[] loadBouquetImageBytes(Bouquet bouquet) throws IOException {
        String bouquetImageUrl = bouquet.getImageUrl();

        if (bouquetImageUrl == null || bouquetImageUrl.isBlank()) {
            throw new IllegalStateException("Bouquet image URL is empty. bouquetId=" + bouquet.getId());
        }

        String relative = bouquetImageUrl.substring("/images/".length()); // "bouquets/bouquet000.jpg"
        Path path = Paths.get(uploadDir, relative);
        return Files.readAllBytes(path);
    }

    private String callFastApiComposite(MultipartFile userImageFile, byte[] bouquetImageBytes) {
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();

        // user_image
        bodyBuilder.part("user_image", userImageFile.getResource())
                .filename(userImageFile.getOriginalFilename())
                .contentType(userImageFile.getContentType() == null
                        ? MediaType.IMAGE_JPEG
                        : MediaType.parseMediaType(userImageFile.getContentType()));

        // bouquet_image
        ByteArrayResource bouquetResource = new ByteArrayResource(bouquetImageBytes) {
            @Override
            public String getFilename() {
                return "bouquet.jpg";
            }
        };

        bodyBuilder.part("bouquet_image", bouquetResource)
                .filename("bouquet.jpg")
                .contentType(MediaType.IMAGE_JPEG);

        FastApiCompositeResponse fastRes = fastapiWebClient.post()
                .uri("/api/composite-bouquet")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .retrieve()
                .bodyToMono(FastApiCompositeResponse.class)
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

        // ★ 업로드 폴더 기준 상대 경로를 /images/** URL 로 반환
        String relativePath = "apply/gen/" + filename;           // 업로드 디렉터리 기준
        return "/images/" + relativePath.replace("\\", "/");      // 윈도우 대비 슬래시 통일
    }

    /**
     * ApplyImage -> ApplyImageResponse 매핑
     * - BouquetResponse (카테고리 포함) 함께 내려줌
     */
    private ApplyImageResponse toResponse(ApplyImage entity) {
        Bouquet bouquet = entity.getBouquet();

        // 부케 + 카테고리 DTO 생성
        BouquetResponse bouquetResponse = toBouquetResponse(bouquet);

        return new ApplyImageResponse(
                entity.getId(),
                entity.getUser().getId(),
                bouquet.getId(),
                entity.getSession() != null ? entity.getSession().getId() : null,
                entity.getSrcImageUrl(),
                entity.getGenImageUrl(),
                entity.getStatus(),
                entity.getCreatedAt(),
                bouquetResponse
        );
    }

    private BouquetResponse toBouquetResponse(Bouquet bouquet) {
        BouquetCategory categories = bouquetCategoryRepository.findByBouquet(bouquet);
        BouquetCategoryResponse categoryResponses = toCategoryResponse(categories);

        return BouquetResponse.builder()
                .id(bouquet.getId())
                .name(bouquet.getName())
                .price(bouquet.getPrice())
                .reason(bouquet.getReason())
                .description(bouquet.getDescription())
                .imageUrl(bouquet.getImageUrl())
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

    private record FastApiCompositeResponse(
            String status,
            String original_user_file,
            String original_bouquet_file,
            String result_image_url
    ) {}
}
