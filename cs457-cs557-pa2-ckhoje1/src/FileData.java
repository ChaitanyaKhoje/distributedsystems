public class FileData {

    private String content;
    private String contentHash;

    public FileData(String contentIn, String contentHashIn) {
        content = contentIn;
        contentHash = contentHashIn;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }
}
