package edu.campbellsville.lablink.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Adds "currentPath" to every page's model so the nav bar can highlight the section the user is in
 * (backlog 3.7). A @ControllerAdvice applies to all controllers, so no controller has to remember it.
 */
@ControllerAdvice
public class NavigationAdvice {

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return pathWithinApp(request.getRequestURI(), request.getContextPath());
    }

    /** "/lablink/labs/3/edit" with context path "/lablink" becomes "/labs/3/edit". */
    static String pathWithinApp(String uri, String contextPath) {
        if (uri == null) {
            return "/";
        }
        String path = contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)
                ? uri.substring(contextPath.length()) : uri;
        return path.isEmpty() ? "/" : path;
    }
}
