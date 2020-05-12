/**
 * @author Marco Pedrosa
 * @author Vasco Patrício
 */
import generated.*;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.GregorianCalendar;
import java.util.List;
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
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

/**
 * Esta classe corresponde a uma aplicação que está sempre a correr a espera de
 * mensagens vindas do JMS, e vai actualizando um contador com o número de
 * noticias das ultimas 12 horas. De cada vez que recebe a mensagem actualiza
 * esse valor num ficheiro no disco, do modo a quando voltar a correr ser
 * apresentada o valor antigo, mesmo que não tenha recebidas novas mensagens a
 * actualizar este valor. Como é para estar sempre ligado a espera de mensagens
 * vamos implementar um MessageListener que recebe mensagens de forma assincrona
 * 
 * @author Marco Pedrosa
 * @author Vasco Patrício
 */
public class StatsProducer implements MessageListener {
	TopicConnection conn = null;
	TopicSession session = null;
	Topic topic = null;
	private int newsunder12hours = 0;
	private String counter_StatsProducer_filename = "StatsProducer.dat";

	public static void main(String args[]) throws Exception {
		StatsProducer client = new StatsProducer();
		System.out.println("A inicializar o StatsProducer...");
		try {
			client.initJMSService();
		} catch (Exception e) {
			System.out
					.println("Erro a inicializar o Serviço de JMS. Verificar se o Wildfly está ligado e se o tópico está disponivel.");
			// e.printStackTrace();
			System.exit(0);
		}
		System.out.println("StatsProducer inicializado com sucesso...");
		System.out
				.println("A carregar o contador de noticias da última execução do StatsProducer...");
		File logcrawler = new File(client.counter_StatsProducer_filename);
		if (logcrawler.exists()) {
			try {
				client.newsunder12hours = client.loadCounter();
				System.out.println("De acordo com a última mensagem obtida:");
				System.out.println("Número de Notícias com menos de 12 horas: "
						+ client.newsunder12hours + ".");
			} catch (Exception e) {
				System.out
						.println("Erro a carregar o contador de noticias da última execução do StatsProducer.");
				e.printStackTrace();
			}
		} else {
			System.out
					.println("Não foi encontrado o contador de noticias da última execução. (StatsProducer.dat).");
			System.out.println("Número de Notícias com menos de 12 horas: 0.");
		}
		// vamos esperar por um enter para terminar a aplicação, senão a
		// aplicação irá terminar
		// antes sequer de receber mensagens
		System.out
				.println("A espera de mensagens - Se quiser encerrar o StatsProducer carregue em \"enter\" na consola.");
		System.in.read();
		System.out.println("A encerrar o StatsProducer...");
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
		if (this.processMessage(tmsg)) {
			System.out.println("Número de Notícias com menos de 12 horas: "
					+ this.newsunder12hours + ".");
			try {
				System.out
						.println("A actualizar o ficheiro do contador de noticias da última execução do StatsProducer...");
				saveCounter(this.newsunder12hours);
				System.out
						.println("Actualizado com sucesso o ficheiro do contador de noticias da última execução do StatsProducer...");
			} catch (Exception e) {
				System.out
						.println("Erro a actualizar o ficheiro do contador de noticias da última execução do StatsProducer...");
				e.printStackTrace();
			}
		}
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
		// caso verdadeiro passar as noticias todas, verificar se a data tem
		// menos de 12 horas, e actualizar o contador
		this.newsunder12hours = 0;
		this.newsunder12hours = this.newsunder12hours
				+ this.countNewsUnder12Hours(
						newslist.getNewsUs().getNewsItem(), "Us");
		this.newsunder12hours = this.newsunder12hours
				+ this.countNewsUnder12Hours(newslist.getNewsAfrica()
						.getNewsItem(), "Africa");
		this.newsunder12hours = this.newsunder12hours
				+ this.countNewsUnder12Hours(newslist.getNewsAsia()
						.getNewsItem(), "Asia");
		this.newsunder12hours = this.newsunder12hours
				+ this.countNewsUnder12Hours(newslist.getNewsEurope()
						.getNewsItem(), "Europe");
		this.newsunder12hours = this.newsunder12hours
				+ this.countNewsUnder12Hours(newslist.getNewsLatinamerica()
						.getNewsItem(), "Latinamerica");
		this.newsunder12hours = this.newsunder12hours
				+ this.countNewsUnder12Hours(newslist.getNewsMiddleeast()
						.getNewsItem(), "Middleeast");
		return true;
	}

	public int countNewsUnder12Hours(List<NewsItem> newslist, String region_id) {
		int numbernews = 0;
		try {
			for (NewsItem news : newslist) {
				try {
					XMLGregorianCalendar datenews = news.getDate();
					GregorianCalendar gregorianCalendar = new GregorianCalendar();
					DatatypeFactory datatypeFactory = DatatypeFactory
							.newInstance();
					XMLGregorianCalendar datenow = datatypeFactory
							.newXMLGregorianCalendar(gregorianCalendar);
					Duration difference = datatypeFactory.newDuration(datenow
							.toGregorianCalendar().getTimeInMillis()
							- datenews.toGregorianCalendar().getTimeInMillis());
					int years = difference.getYears();
					int months = difference.getMonths();
					int days = difference.getDays();
					int hours = difference.getHours();
					// System.out.println("Current Date is "+datenow);
					// System.out.println("News Date is "+datenews);
					// System.out.println("Difference in years is "+years+"Difference in months is "+months+"Difference in days is "+days+"Difference in hours is "+hours);
					if (years == 0 && months == 0 && days == 0 && hours <= 12) {
						numbernews++;
					}
				} catch (Exception e) {
					System.out
							.println("Erro a fazer verificar a diferença da data numa noticia da região "
									+ region_id + ".");
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			System.out.println("Erro com a lista de notícias da região "
					+ region_id + ".");
			e.printStackTrace();
		}
		return numbernews;
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
		conn.setClientID("statsproducer");
		topic = (Topic) iniCtx.lookup("jms/topic/cnnNews");
		session = conn
				.createTopicSession(true, TopicSession.SESSION_TRANSACTED);
		// session = conn.createTopicSession(false,
		// TopicSession.AUTO_ACKNOWLEDGE);
		// Como estamos a implementar o MessageListener temos de subscrever ao
		// topico antes de receber as mensagens
		// 2º argumento identifica a subscricao
		TopicSubscriber recv = session.createDurableSubscriber(topic,
				"jms-statsproducer");
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
	 * Método que vai obter de um ficheiro o contador com o número de mensagens
	 * com menos de 12 horas na altura da última execução do WebCrawler. Este
	 * irá conter um Objecto Java com um Integer resultante resultante do
	 * tratamento de uma execução do WebCrawler.
	 * 
	 * @author Marco Pedrosa, Vasco Patrício
	 * @return int com o número de noticias com menos de 12 horas na altura da
	 *         última execução do WebCrawler.
	 */
	public int loadCounter() throws IOException, ClassNotFoundException,
			ClassCastException {
		Integer result;
		InputStream file = new FileInputStream(counter_StatsProducer_filename);
		InputStream buffer = new BufferedInputStream(file);
		ObjectInput input = new ObjectInputStream(buffer);
		try {
			// deserialize the Object
			result = (Integer) input.readObject();
		} finally {
			input.close();
			buffer.close();
			file.close();
		}
		return result.intValue();
	}

	/**
	 * Método que vai gravar para um ficheiro o contador com o número de
	 * mensagens com menos de 12 horas na altura da última execução do
	 * WebCrawler. Este irá conter um Objecto Java com um Integer resultante
	 * resultante do tratamento de uma execução do WebCrawler.
	 * 
	 * @author Marco Pedrosa, Vasco Patrício
	 * @param int com o número de noticias com menos de 12 horas na altura da
	 *        última execução do WebCrawler.
	 */
	public void saveCounter(int counterlastexecution) throws IOException {
		OutputStream file = new FileOutputStream(counter_StatsProducer_filename);
		OutputStream buffer = new BufferedOutputStream(file);
		ObjectOutput output = new ObjectOutputStream(buffer);
		try {
			// write to file
			output.writeObject(new Integer(counterlastexecution));
		} finally {
			output.close();
			buffer.close();
			file.close();
		}
	}
}