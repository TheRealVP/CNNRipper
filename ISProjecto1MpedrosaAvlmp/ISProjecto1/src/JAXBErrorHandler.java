import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class JAXBErrorHandler implements ErrorHandler {
	public boolean showerrorsconsole=false;
	public boolean throwexception=true;
	
	public void warning(SAXParseException exception) throws SAXException {
		if(showerrorsconsole) {
			System.out.println("\nWARNING");
			exception.printStackTrace();
		}
		if(throwexception)
			throw exception;
    }
	public void error(SAXParseException exception) throws SAXException {
		if(showerrorsconsole) {
			System.out.println("\nERROR");
			exception.printStackTrace();
		}
		if(throwexception)
			throw exception;
    }
	public void fatalError(SAXParseException exception) throws SAXException {
		if(showerrorsconsole) {
			System.out.println("\nFATAL ERROR");
			exception.printStackTrace();
		}
		if(throwexception)
			throw exception;
    }
}
