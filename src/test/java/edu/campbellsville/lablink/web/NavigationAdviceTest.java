package edu.campbellsville.lablink.web;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/** UNIT tests for the path used to highlight the current nav link (backlog 3.7). */
@Tag("unit")
class NavigationAdviceTest {

    private final NavigationAdvice advice = new NavigationAdvice();

    @Test
    void pathWithoutContextPathIsUnchanged() {
        assertThat(NavigationAdvice.pathWithinApp("/labs/3/edit", "")).isEqualTo("/labs/3/edit");
    }

    @Test
    void contextPathIsStripped() {
        assertThat(NavigationAdvice.pathWithinApp("/lablink/equipment", "/lablink")).isEqualTo("/equipment");
    }

    @Test
    void appRootBecomesSlash() {
        assertThat(NavigationAdvice.pathWithinApp("/lablink", "/lablink")).isEqualTo("/");
        assertThat(NavigationAdvice.pathWithinApp(null, null)).isEqualTo("/");
    }

    @Test
    void readsPathFromTheRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/reservations/manage");

        assertThat(advice.currentPath(request)).isEqualTo("/reservations/manage");
    }
}
