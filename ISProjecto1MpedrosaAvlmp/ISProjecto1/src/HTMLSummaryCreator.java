/**
 * @author Marco Pedrosa
 * @author Vasco Patrício
 */
import generated.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSubscriber;
import javax.jms.TopicSession;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.JAXBSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

/**
 * Esta classe corresponde a uma aplicação que está sempre a correr a espera de
 * mensagens vindas do JMS, e vai verificar a estrutura com um XSD (Schema file).
 * Caso a mensagem XML tenha um formato correspondente ao definido no XSD
 * vamos gravar essa mensagem em XML no disco para poder ser visualizada em
 * html através de um browser com motor de XSLT integrado como o Firefox.
 * Como é para estar sempre ligado a espera de mensagens
 * vamos implementar um MessageListener que recebe mensagens de forma assincrona
 * 
 * @author Marco Pedrosa
 * @author Vasco Patrício
 */
public class HTMLSummaryCreator implements MessageListener {
	TopicConnection conn = null;
	TopicSession session = null;
	Topic topic = null;
	private String default_filename = "CnnNews_";
	private String default_filename_extension = ".xml";

	public static void main(String args[]) throws Exception {
		HTMLSummaryCreator client = new HTMLSummaryCreator();
		System.out.println("A inicializar o HTMLSummaryCreator...");
		try {
			client.initJMSService();
		} catch (Exception e) {
			System.out
					.println("Erro a inicializar o Serviço de JMS. Verificar se o Wildfly está ligado e se o tópico está disponivel.");
			// e.printStackTrace();
			System.exit(0);
		}
		System.out.println("HTMLSummaryCreator inicializado com sucesso...");
		// vamos esperar por um enter para terminar a aplicação, senão a
		// aplicação irá terminar
		// antes sequer de receber mensagens
		System.out
				.println("A espera de mensagens - Se quiser encerrar o HTMLSummaryCreator carregue em \"enter\" na consola.");
		System.in.read();
		System.out.println("A encerrar o HTMLSummaryCreator...");
		try {
			client.stopJMSService();
		} catch (Exception e) {
			System.out.println("Erro a encerrar o Serviço de JMS.");
			// e.printStackTrace();
		}
		System.exit(0);
	}

	/**
	 * Método que vai automaticamente corrido (devido ao MessageListener
	 * interface) de cada vez que é enviada uma mensagem
	 * 
	 * @author Marco Pedrosa, Vasco Patrício
	 * @param msg
	 *            Message que vai conter a mensagem recebida através do JMS
	 */
	public void onMessage(Message msg) {
		String tmsg;
		try {
			System.out.println("A receber Mensagem...");
			tmsg = ((TextMessage) msg).getText();
			// System.out.println(tmsg);
			session.commit();
			System.out.println("Mensagem recebida com sucesso.");
		} catch (Exception e) {
			System.out.println("Erro a receber mensagem.");
			e.printStackTrace();
			return;
		}
		this.processMessage(tmsg);
		System.out
				.println("A espera de mensagens - Se quiser encerrar o StatsProducer carregue em \"enter\" na consola.");
	}

	public boolean processMessage(String message) {
		JAXBContext jaxbContext = null;
		CnnNewsList newslist = null;
		try {
			jaxbContext = JAXBContext.newInstance(CnnNewsList.class);
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			StringReader reader = new StringReader(message);
			newslist = (CnnNewsList) jaxbUnmarshaller.unmarshal(reader);
			// System.out.println(newslist.getNewsUs().getNewsItem().size());
		} catch (Exception f) {
			System.out.println("Erro a fazer o Marshall da String.");
			f.printStackTrace();
		}
		// verificar se está de acordo com o xsd
		try {
			JAXBErrorHandler errorhandler = new JAXBErrorHandler();
			// se quisermos ver todos os erros, se quisermos apanhar existirem
			// erros ou não no try catch deixar default
			// errorhandler.showerrorsconsole=true;
			// errorhandler.throwexception=false;
			//
			JAXBSource source = new JAXBSource(jaxbContext, newslist);
			SchemaFactory sf = SchemaFactory
					.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			Schema schema = sf.newSchema(new File("cnn_news_structure.xsd"));
			Validator validator = schema.newValidator();
			validator.setErrorHandler(errorhandler);
			validator.validate(source);
		} catch (Exception e) {
			System.out
					.println("Erro a fazer a fazer a validação com XSD, a ignorar a mensagem.");
			e.printStackTrace();
			return false;
		}
		System.out.println("A gravar no disco a mensagem XML vinda do JMS.");
		try {
			String filename = this.saveMessageFile(message);
			System.out.println("Mensagem XML vinda do JMS gravada no disco com sucesso no ficheiro: "+filename);
		} catch (Exception e) {
			System.out.println("Erro a gravar no disco a mensagem XML vinda do JMS.");
			e.printStackTrace();
		}
		return true;
	}

	/**
	 * Método que vai tratar da inicialização de Objectos necessários para
	 * mandar mensagens para o WildFly. Isto envolve uma TopicConnection, um
	 * Topic, e uma TopicSession
	 * 
	 * @author Marco Pedrosa, Vasco Patrício
	 */
	public void initJMSService() throws NamingException, JMSException {
		InitialContext iniCtx = new InitialContext();
		Object tmp = iniCtx.lookup("jms/RemoteConnectionFactory");
		TopicConnectionFactory tcf = (TopicConnectionFactory) tmp;
		// mesmo que multiplos subscritores se liguem com o mesmo login/password
		// se o identificador do cliente e identificador da subscricao forem
		// diferentes ambos recebem a mensagem como durable subscribers
		conn = tcf.createTopicConnection("joao", "pedro");
		// identifica o cliente desta conecao
		conn.setClientID("htmlsummarycreator");
		topic = (Topic) iniCtx.lookup("jms/topic/cnnNews");
		session = conn
				.createTopicSession(true, TopicSession.SESSION_TRANSACTED);
		// session = conn.createTopicSession(false,
		// TopicSession.AUTO_ACKNOWLEDGE);
		// Como estamos a implementar o MessageListener temos de subscrever ao
		// topico antes de receber as mensagens
		// 2º argumento identifica a subscricao
		TopicSubscriber recv = session.createDurableSubscriber(topic,
				"jms-htmlsummarycreator");
		recv.setMessageListener(this);
		// MessageConsumer mc = session.createConsumer(topic);
		// mc.setMessageListener(this);
		conn.start();
	}

	/**
	 * Método que vai libertar os Objectos necessários para mandar mensagens
	 * para o WildFly. Isto envolve uma TopicConnection, um Topic, e uma
	 * TopicSession
	 * 
	 * @author Marco Pedrosa, Vasco Patrício
	 */
	public void stopJMSService() throws NamingException, JMSException {
		conn.stop();
		session.close();
		conn.close();
	}

	/**
	 * Método que vai gravar para um ficheiro a mensagem XML recebida em
	 * texto pelo JMS.
	 * 
	 * @author Marco Pedrosa, Vasco Patrício
	 * @param message String com a mensagem XML recebida em formato UTF-8 vinda do JMS
	 * @return String com o nome com que o ficheiro ficou no disco
	 */
	public String saveMessageFile(String message) throws Exception {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		Calendar cal = Calendar.getInstance();
		String tmp_filename = this.default_filename
				+ dateFormat.format(cal.getTime())
				+ this.default_filename_extension;
		FileOutputStream fos = new FileOutputStream(tmp_filename);
		OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
		Writer out = new BufferedWriter(osw);
		try {
			out.write(message);
		} finally {
			out.close();
		}
		return tmp_filename;
	}
}
