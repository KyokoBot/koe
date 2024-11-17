package moe.kyokobot.koe.mls.codec;

public class ValidatedContent {
    private final AuthenticatedContent authenticatedContent;

    public ValidatedContent(AuthenticatedContent authenticatedContent) {
        this.authenticatedContent = authenticatedContent;
    }

    public AuthenticatedContent getAuthenticatedContent() {
        return authenticatedContent;
    }
}
