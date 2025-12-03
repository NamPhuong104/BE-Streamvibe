package movieapp.service;

import lombok.extern.slf4j.Slf4j;
import movieapp.dto.HomepageReponse.HomepageGroupResponse;
import movieapp.dto.HomepageReponse.HomepageResponse;
import movieapp.dto.MetaAndHead.SeoOnPage;
import movieapp.dto.OphimResponse.OphimHomepageResponse;
import movieapp.dto.CustomFieldsResponse.MovieItemDTO;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class HomepageService {
    private final CachedSectionService cachedSectionService;

    public HomepageService(CachedSectionService cachedSectionService) {
        this.cachedSectionService = cachedSectionService;
    }

    public HomepageGroupResponse getHomepageByGroup(String group) {
        log.info("🔄 Fetching homepage group: {}", group);
        long startTime = System.currentTimeMillis();

        try {
            HomepageGroupResponse.HomepageGroupResponseBuilder builder = HomepageGroupResponse
                    .builder()
                    .message("Success")
                    .cachedAt(System.currentTimeMillis())
                    .group(group);

            switch (group.toLowerCase()) {
                case "initial":
                    builder.section1(cachedSectionService.fetchSection1())
                            .section2(cachedSectionService.fetchSection2())
                            .section3(cachedSectionService.fetchSection3())
                            .section4(cachedSectionService.fetchSection4())
                            .hasMore(true)
                            .nextGroup("group1")
                            .seoOnPage(new SeoOnPage());
                    break;
                case "group1":
                    builder.section5(cachedSectionService.fetchSection5())
                            .section6(cachedSectionService.fetchSection6())
                            .section7(cachedSectionService.fetchSection7())
                            .section8(cachedSectionService.fetchSection8())
                            .hasMore(true)
                            .nextGroup("group2");
                    break;
                case "group2":
                    builder.section9(cachedSectionService.fetchSection9())
                            .section10(cachedSectionService.fetchSection10())
                            .section11(cachedSectionService.fetchSection11())
                            .section12(cachedSectionService.fetchSection12())
                            .hasMore(false)
                            .nextGroup(null);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid group: " + group);
            }
            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ Group {} loaded in {}ms", group, duration);

            return builder.build();
        } catch (Exception e) {
            log.error("❌ Failed to fetch group {}: {}", group, e.getMessage());
            throw new RuntimeException("Failed to fetch homepage group: " + group, e);
        }
    }

    public HomepageResponse getHomepageData() {
        log.info("🔄 Fetching FULL homepage data (12 sections) from Ophim API...");
        long startTime = System.currentTimeMillis();

        try {
            OphimHomepageResponse rawData = cachedSectionService.fetchHomepageRaw();
            List<MovieItemDTO> section1 = cachedSectionService.fetchSection1();
            HomepageResponse.Section2Data section2 = cachedSectionService.fetchSection2();
            List<MovieItemDTO> section3 = cachedSectionService.fetchSection3();   // Series
            List<MovieItemDTO> section4 = cachedSectionService.fetchSection4();   // Action
            List<MovieItemDTO> section5 = cachedSectionService.fetchSection5();   // Single
            List<MovieItemDTO> section6 = cachedSectionService.fetchSection6();   // New Movie
            List<MovieItemDTO> section7 = cachedSectionService.fetchSection7();   // Horror
            List<MovieItemDTO> section8 = cachedSectionService.fetchSection8();   // Korea Love
            List<MovieItemDTO> section9 = cachedSectionService.fetchSection9();   // Cartoon - CÓ content
            List<MovieItemDTO> section10 = cachedSectionService.fetchSection10(); // Crime
            List<MovieItemDTO> section11 = cachedSectionService.fetchSection11(); // Secret
            List<MovieItemDTO> section12 = cachedSectionService.fetchSection12(); // Adventure


            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ Homepage built in {}ms ({}s)", duration, duration / 1000);

            return HomepageResponse.builder()
                    .seoOnPage(new SeoOnPage())
                    .message("Success")
                    .cachedAt(System.currentTimeMillis())
                    .rawData(rawData)
                    .section1(section1)
                    .section2(section2)
                    .section3(section3)
                    .section4(section4)
                    .section5(section5)
                    .section6(section6)
                    .section7(section7)
                    .section8(section8)
                    .section9(section9)
                    .section10(section10)
                    .section11(section11)
                    .section12(section12)
                    .build();
        } catch (Exception e) {
            log.error("");
            throw new RuntimeException("Failed to fetch homepage data", e);
        }
    }
}
