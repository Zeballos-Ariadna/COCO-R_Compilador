package CocoRFiles.Program;

public class main {
    public static void main(String[] args) {
        String xml_filename = "src/CocoRFiles/examples/note2.xml";
        System.out.println("Parsing file: \"" + xml_filename + "\"" );
        Scanner scanner = new Scanner( xml_filename );
        Parser parser = new Parser( scanner );
        parser.Parse();
    }
}
