package ca.carleton.michelleburrows.networkmonitor;

/**
 * An item to insert into an accordion view.
 * Contains a header (line of text that is always visible) and expandable collapsible contents.
 * Created by Michelle on 3/21/2015.
 */
public class AccordionItem {
    private String header;
    private String contents;

    public AccordionItem(String header, String contents) {
        this.header = header;
        this.contents = contents;
    }

    public String getContents() {
        return contents;
    }

    public void setContents(String contents) {
        this.contents = contents;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }
}
