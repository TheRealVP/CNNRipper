/**
 * @author Marco Pedrosa
 * @author Vasco Patrício
 */
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
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.StringTokenizer;
import generated.*;
import javax.jms.JMSException;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.bind.JAXBContext;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

public class WebCrawler {

	private Document document;
	private static CnnNewsList news;
	TopicConnection conn = null;
	TopicSession session = null;
	Topic topic = null;
	private String log_WebCrawler_filename = "WebCrawlerlog.dat";

	public WebCrawler(Document document) {
		this.document = document;
	}

	public static void main(String[] args) throws IOException {
		WebCrawler screenScraper=null;
		StringWriter writer=null;
		
		// fetchVideo("http://edition.cnn.com/2014/10/15/us/alcs-kansas-city-royals-win/index.html");
		news = new CnnNewsList();

		news.getNewsAfrica().getNewsItem();
		news.getNewsAsia().getNewsItem();
		news.getNewsEurope().getNewsItem();
		news.getNewsLatinamerica().getNewsItem();
		news.getNewsUs().getNewsItem();
		news.getNewsMiddleeast().getNewsItem();
		// news.getNewsItem();
		ArrayList<String> foundurls = new ArrayList<String>();
		try {
			Document document = Jsoup.connect("http://www.cnn.com/")
					.userAgent("Mozilla").timeout(10000).get();
			screenScraper = new WebCrawler(document);
			// Elements hrefs= screenScraper.document.select("a");
			// for(int i=0; i< hrefs.size();i++)
			// System.out.println(hrefs.get(i).attr("href"));
			Elements out = screenScraper.document
					.getElementsByClass("cnn_ftrnvlnks");
			String buf;
			for (int i = 3; i < out.select("a").size(); i++) {
				buf = out.select("a").get(i).attr("href").toString();
				if (buf.equals("/BUSINESS/")) {
					break;
				}
				runURLs(buf, foundurls);

			}
			try {
				JAXBContext jaxbContext = JAXBContext
						.newInstance(CnnNewsList.class);
				Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
				// output pretty printed
				jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,
						true);
				//jaxbMarshaller.setProperty(Marshaller.JAXB_ENCODING, "ISO-8859-15");
				jaxbMarshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
				writer = new StringWriter();
				//writer.write("<?xml version='1.0' encoding=\"ISO-8859-15\"?>\n");
				writer.write("<?xml version='1.0' encoding=\"UTF-8\"?>\n");
				writer.write("<?xml-stylesheet type=\"text/xsl\" href=\"DeiNewsCNNRipoff.xsl\"?>\n");
				writer.write("<!-- Generated automatically. Don't change it. -->\n");
				jaxbMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
				jaxbMarshaller.marshal(news, writer);
				System.out.println(writer.toString());
			} catch (Exception f) {
				System.out.println("ERROR in Marshalling");
				f.printStackTrace();
				System.exit(0);
			}
		} catch (IOException e) {
			System.out.println("ERROR in Connection");
			e.printStackTrace();
			System.exit(0);
		}
		screenScraper.publish(writer.toString());
	}
     /**
	 * Método que percorre as notícias por cada headline. 
	 * 
	 * @author Marco Pedrosa, Vasco Patr?cio
	 * @param buf sufixo de http://cnn.com que indica a headline
     * @param foundurls lista de URLs visitados, para evitar retirar dados
     * da mesma notícia
	 */
	private static void runURLs(String buf, ArrayList<String> foundurls) {
		try {
			System.out.println("http://www.cnn.com" + buf);
			Document newstypes = Jsoup.connect("http://www.cnn.com" + buf)
					.userAgent("Mozilla").timeout(6000).get();
			WebCrawler newsScraper = new WebCrawler(newstypes);
			Elements newsLinks = newsScraper.document
					.getElementsByClass("cnn_mtt1imghtitle");
			StringTokenizer tk = new StringTokenizer(buf, "/");
			String region = tk.nextToken().toLowerCase();
			catchNews(newsLinks, foundurls, region);
			newsLinks = newsScraper.document
					.getElementsByClass("cnn_sectt1cntnt");
			catchNews(newsLinks, foundurls, region);
			// getXML("http://www.cnn.com"+newsLinks.select("a").first().attr("href").toString());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
        /**
	 * Método que filtra as notícias válidas e adiciona-as. Este método
     * tem como função secundária actualizar o número de URLs processados
	 * 
	 * @author Marco Pedrosa, Vasco Patr?cio
	 * @param newsLinks elementos contendo os URLs a visitar
     * @param foundurls lista de URLs visitados, para evitar retirar dados
     * da mesma notícia
     * @param region região em questão
	 */
	private static void catchNews(Elements newsLinks,
			ArrayList<String> foundurls, String region) {
		NewsItem nu;
		for (int j = 0; j < newsLinks.select("a").size() /*1*/; j++) {
			System.out.println("    "
					+ newsLinks.select("a").get(j).attr("href"));
			if ((newsLinks.select("a").get(j).attr("href")
					.contains("http://www.cnn.com") || newsLinks.select("a")
					.get(j).attr("href").contains("http://edition.cnn.com"))
					&& !(newsLinks.select("a").get(j).attr("href")
							.contains("http://money.cnn.com/"))
					&& !(newsLinks.select("a").get(j).attr("href")
							.contains("http://www.cnn.com/interactive/"))
					&& !(newsLinks.select("a").get(j).attr("href")
							.contains("http://ireport"))
					&& !(newsLinks.select("a").get(j).attr("href")
							.contains("http://cnnphotos.blogs"))
					&& !(newsLinks.select("a").get(j).attr("href")
							.contains("http://religion.blogs"))
					&& !(newsLinks.select("a").get(j).attr("href")
							.contains("http://cnnespanol"))
					&& !(foundurls.contains(newsLinks.select("a").get(j)
							.attr("href")))
					&& !(newsLinks.select("a").get(j).attr("href")
							.contains("cnn.com/SPECIALS/"))
					&& !(newsLinks.select("a").get(j).attr("href")
							.contains("http://www.cnn.com/video/"))) {
				if (foundurls.contains(newsLinks.select("a").get(j)
						.attr("href"))) {
					System.out.println("Address found!!!!");
				} else {
					System.out.println("Long address found!");
					nu = getXML(newsLinks.select("a").get(j).attr("href"), 0);
					// System.out.println(nu.getHighlights());
					if (nu != null) {
						foundurls
								.add(newsLinks.select("a").get(j).attr("href"));
						newsSplitter(nu, region);
					}
				}
			} else if (!(newsLinks.select("a").get(j).attr("href")
					.contains("/interactive/"))
					&& !(newsLinks.select("a").get(j).attr("href")
							.contains("http://ireport"))
					&& !(newsLinks.select("a").get(j).attr("href")
							.contains("http://cnnphotos.blogs"))
					&& !(newsLinks.select("a").get(j).attr("href")
							.contains("http://religion.blogs"))
					&& !(newsLinks.select("a").get(j).attr("href")
							.contains("http://money.cnn.com/"))
					&& !(newsLinks.select("a").get(j).attr("href")
							.contains("http://cnnespanol"))
					&& !(foundurls.contains(newsLinks.select("a").get(j)
							.attr("href")))
					&& !(newsLinks.select("a").get(j).attr("href")
							.contains("cnn.com/SPECIALS/"))
					&& !(newsLinks.select("a").get(j).attr("href")
							.contains("/video/"))) {
				if (foundurls.contains(newsLinks.select("a").get(j)
						.attr("href"))) {
					System.out.println("Address found!!!!");
				} else {
					nu = getXML("http://www.cnn.com"
							+ newsLinks.select("a").get(j).attr("href"), 0);
					// System.out.println(nu.getHighlights());
					if (nu != null) {
						foundurls
								.add(newsLinks.select("a").get(j).attr("href"));
						newsSplitter(nu, region);
					}
				}
			}
		}

	}
	/*
	 * Método que coloca as notícias nas suas respetivas regiões.
	 * 
	 * @author Marco Pedrosa, Vasco Patr?cio
	 * @param item objeto com a informação da notícia
     * @param region região relevante
	 */
	private static void newsSplitter(NewsItem item, String region) {
		if (region.equalsIgnoreCase("us")) {
			NewsUs nus = news.getNewsUs();
			nus.setNewsItem(item);
			news.setNewsUs(nus);
		} else if (region.equalsIgnoreCase("africa")) {
			NewsAfrica naf = news.getNewsAfrica();
			naf.setNewsItem(item);
			news.setNewsAfrica(naf);
		} else if (region.equalsIgnoreCase("asia")) {
			NewsAsia nas = news.getNewsAsia();
			nas.setNewsItem(item);
			news.setNewsAsia(nas);
		} else if (region.equalsIgnoreCase("europe")) {
			NewsEurope nep = news.getNewsEurope();
			nep.setNewsItem(item);
			news.setNewsEurope(nep);
		} else if (region.equalsIgnoreCase("latinamerica")) {
			NewsLatinamerica nlat = news.getNewsLatinamerica();
			nlat.setNewsItem(item);
			news.setNewsLatinamerica(nlat);
		} else if (region.equalsIgnoreCase("middleeast")) {
			NewsMiddleeast nmed = news.getNewsMiddleeast();
			nmed.setNewsItem(item);
			news.setNewsMiddleeast(nmed);
		}
	}

     /**
	 * Método que retira dados da página de notícias. Assume-se que a página 
	 * está escrita de acordo com o formato padrão de notícias da CNN.
     * Campos inválidos ou não-existentes não são adicionados.
	 * 
	 * @author Marco Pedrosa, Vasco Patr?cio
	 * @param msg URL da página de notícias
     * @param retries Quantidade de tentativas de estabelecer a coneção
	 */
	public static NewsItem getXML(String url, int retries) {
		try {
			System.out.println("Trying to read " + url);
			Document rawnews = Jsoup.connect(url).userAgent("Mozilla")
					.timeout(6000).get();
			/*
			 * if(rawnews.select("meta[name=medium]").first().attr("content").
			 * isEmpty()) {
			 */
			NewsItem nus = new NewsItem();
			nus.setUrl(url);
			Photos shots = new Photos();
			shots.getPhoto();
			Videos vids = new Videos();
			vids.getVideo();
			String title = rawnews.getElementsByTag("title").html();
			nus.setTitle(title);
			// System.out.println(title);
			/*
			 * if (rawnews.select("meta[name=pubdate]").isEmpty()) { Pattern p =
			 * Pattern.compile("/[0-9]++/[0-9]++/[0-9]++"); Matcher matcher =
			 * p.matcher(url); if (matcher.find()) {
			 * //System.out.println(matcher.group(0)); //prints /{item}/
			 * StringTokenizer tk = new StringTokenizer(matcher.group(0), "/");
			 * String date = tk.nextToken() + "-" + tk.nextToken() + "-" +
			 * tk.nextToken() + "T00:00:00Z"; nus.setDate(getXmlDateTime(date));
			 * } } else {
			 */
			String date = rawnews.select("meta[name=pubdate]").first()
					.attr("content").toString();
			nus.setDate(getXmlDateTime(date));
			// }
			if (!rawnews.select("meta[name=author]").isEmpty()) {
				if (!rawnews.select("meta[name=author]").first()
						.attr("content").equals("")) {
					String author = rawnews.select("meta[name=author]").first()
							.attr("content");
					// System.out.println(author);
					nus.setAuthor(author);
				}
			}
			Elements hilites = rawnews.getElementsByClass("cnn_strylftcntnt");
			if (hilites != null) {
				String hlts = hilites
						.select("ul[class=cnn_bulletbin cnnStryHghLght]")
						.select("li").text();
				nus.setHighlights(hlts);
			}
			ArrayList<String> vods = fetchVideo(url);
			if (vods != null) {
				vids.setVideo(vods);
				nus.setVideos(vids);
			}
			Elements pars = rawnews.select("p").val("cnn_storypgraphtxt");
			// System.out.println(pars.text());
			nus.setText(pars.text());
			Elements photos = rawnews
					.getElementsByClass("cnnArticleGalleryPhotoContainer");
			if (photos.size() > 0) {
				for (int i = 0; i < photos.select("img").size(); i++) {
					String anothershot = photos.select("img").get(i)
							.attr("src").toString();
					// System.out.println(anothershot);
					shots.setPhoto(anothershot);
				}
				nus.setPhotos(shots);
			}
			// nus.setPhotos(shots);
			return nus;
			// }
		} catch (Exception e) {
			System.out.println("ERROR in Reading News: " + url);
			e.printStackTrace();
			retries++;
			if (retries < 3) {
				System.out.println("Retry number: " + retries);
				getXML(url, retries);
			}
		}
		return null;
	}
     /**
	 * Método que devolve os URLs dos repositórios onde estão contidos os
     * vídeos. 
     * 
	 * @author Marco Pedrosa, Vasco Patr?cio
	 * @param url URL do repositório
	 */
	public static ArrayList<String> fetchVideo(String url) {
		try {
			ArrayList<String> ret = new ArrayList<String>();
			Document doc = Jsoup.connect(url).userAgent("Mozilla")
					.timeout(6000).get();
			Elements vidments = doc.getElementsByClass("OUTBRAIN");
			Document xml = Jsoup.connect(vidments.attr("data-src"))
					.userAgent("Mozilla").timeout(6000).get();
			for (int k = 0; k < xml.select("file").size(); k++) {
				ret.add(xml.select("file").get(k).html());
			}
			return ret;
		} catch (Exception e) {
			System.out.println("Can't fetch video " + e.toString());
		}
		return null;
	}
     /**
	 * Método que obtém a data e tempo formatado para XML
	 * 
	 * @author Marco Pedrosa, Vasco Patr?cio
	 * @param date data a formatar
	 */
	public static XMLGregorianCalendar getXmlDateTime(String date)
			throws DatatypeConfigurationException {
		return DatatypeFactory.newInstance().newXMLGregorianCalendar(date);
	}

	/**
	 * Método que vai tratar do envio de mensagens para o WildFly. Começa por
	 * Inicializar os objectos necessários para o JMS, depois tentar obter o Log
	 * com a lista de mensagens com envio falhado, depois tenta enviar estas
	 * mensagens, e removê-las do Log, depois tenta enviar a mensagem da
	 * execução actual. Mensagens cujo envio falha voltam a tentar ser enviadas.
	 * 
	 * @author Marco Pedrosa, Vasco Patrício
	 * @param message
	 *            String com documentos xml que contém a lista inteira de
	 *            noticias que corresponde a uma execução do WebCrawler
	 */
	public void publish(String message) {
		System.out.println("A inicializar o Serviço de JMS do WebCrawler...");
		try {
			this.initJMSService();
		} catch (Exception e) {
			System.out
					.println("Erro a inicializar o Serviço de JMS. Verificar se o Wildfly está ligado e se o tópico está disponivel.");
			//e.printStackTrace();
		}
		// procurar e enviar old messages no log
		ArrayList<String> oldmessages = new ArrayList<String>();
		File logcrawler = new File(log_WebCrawler_filename);
		if (logcrawler.exists()) {
			System.out
					.println("A carregar lista de mensagens por enviar do Log do WebCrawler.");
			try {
				oldmessages = this.loadWebCrawlerLog();
				if (oldmessages == null)
					oldmessages = new ArrayList<String>();
				System.out
						.println("Obtida a lista de mensagens por enviar do Log do WebCrawler. Contém "
								+ oldmessages.size() + " mensagens por enviar.");
				System.out.println("Contém " + oldmessages.size()
						+ " mensagens por enviar.");
			} catch (Exception e) {
				System.out
						.println("Erro a carregar o Log de mensagens por enviar do WebCrawler.");
				e.printStackTrace();
			}
		} else {
			System.out
					.println("Não foi encontrado o ficheiro de Log do WebCrawler WebCrawlerlog.dat. Não devem existir mensagens por enviar.");
		}
		// enviar mensagens antigas existentes no log
		int nretries = 5;
		long sleeptimebetweenfail = 1000;
		boolean resultadoenvio;
		int log_numbermessages=1;
		for (int i = 0; i < oldmessages.size(); i++) {
			// tentar enviar x vezes. Se continuar a falhar tentar mandar mensagem seguinte.
			resultadoenvio = this.sendMessageTopic(oldmessages.get(i), "Mensagem "+log_numbermessages+" do Log", nretries, sleeptimebetweenfail);
			if(resultadoenvio) {
				oldmessages.remove(i);
				// because removing message would shift left the next message, and next message will have the same index as this one after removing, so i need to decrement the index
				i--;
			}
			log_numbermessages++;
		}
		// enviar mensagem actual, caso falhe adicionar ao Log de Mensagens para enviar posteriormente
		resultadoenvio = this.sendMessageTopic(message, "Mensagem da Execução actual do WebCrawler", nretries, sleeptimebetweenfail);
		if(!resultadoenvio) {
			System.out.println("Mensagem da Execução actual do WebCrawler. A adicionar mensagem ao Log.");
			oldmessages.add(message);
		}
		try {
			System.out.println("A tentar gravar o Log de Mensagens.");
			this.saveWebCrawlerLog(oldmessages);
		} catch (Exception e) {
			System.out.println("Erro a gravar o Log de Mensagens.");
			e.printStackTrace();
		}
		System.out.println("A encerrar o WebCrawler...");
		try {
			this.stopJMSService();
		} catch (Exception e) {
			System.out.println("Erro a encerrar o Serviço de JMS.");
			//e.printStackTrace();
		}
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
		conn = tcf.createTopicConnection("joao", "pedro");
		topic = (Topic) iniCtx.lookup("jms/topic/cnnNews");
		//true em transacted boolean, TopicSession.SESSION_TRANSACTED(0) em acknowledge
		session = conn.createTopicSession(true, TopicSession.SESSION_TRANSACTED);
		// sem usar transaccoes
		//session = conn.createTopicSession(false, TopicSession.AUTO_ACKNOWLEDGE);
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
	 * Método que vai mandar uma mensagem de texto de forma assincrono para um
	 * tópico no WildFly. Todos os clientes que estiver subscritos com uma
	 * Durable Subscription irão receber a mensagem quando se ligarem ao Wildfly
	 * para recepção de mensagens. Caso dê erro este envio o WebCrawler irá
	 * guardar a mensagem num Log (ficheiro que contém um objecto List<String>
	 * com lista de mensagens falhadas para enviar posteriormente quando o
	 * WebCrawler voltar a executar.
	 * 
	 * @author Marco Pedrosa, Vasco Patrício
	 * @param message
	 *            String com documentos xml que contém a lista inteira de
	 *            noticias que corresponde a uma execução do WebCrawler
	 * @param messageidentifier
	 *            String identificadora da mensagem para output. (Mensagem 1 do
	 *            Log; Mensagem da execução actual)
	 * @param nretries
	 *            Número de vezes que tenta reenviar a mensagem até desistir
	 * @return Boolean com true caso mensagem seja enviada (com 1 tentativa ou
	 *         mais), False caso contrário.
	 */
	public boolean sendMessageTopic(String message, String messageidentifier,
			int nretries, long sleeptime) {
		boolean sentsucessfull = false;
		System.out.println("A tentar enviar -> " + messageidentifier);
		for (int i = 0; i < nretries && sentsucessfull == false; i++) {
			// caso i>0 então houve
			if (i > 0) {
				try {
					Thread.sleep(sleeptime);
				} catch (InterruptedException e) {
					System.out
							.println("Erro no sleep entre erro ao enviar mensagem e voltar a tentar.");
				}
				System.out.println("A tentar re-enviar pela " + i + "ª vez -> "
						+ messageidentifier);
			}
			// Setup the pub/sub connection, session
			// Send a text msg
			try {
				TopicPublisher send = session.createPublisher(topic);
				TextMessage tm = session.createTextMessage(message);
				send.publish(tm);
				//caso tenhamos usado transações em vez de AUTO_ACKNOWLEDGE precisamos de fazer o commit manualmente
				session.commit();
				//nota - antes de usar sessoes dava um "erro" no futuretask. Parecia ser um warning, porque a mensagem era enviada correctamente a mesma, e não parava a execução do método
				//além disso o stacktrace não apontava para o nosso código, mas só directamente para as implementações do JMS
				/*
				Out 17, 2014 4:03:00 PM org.jboss.remoting3.spi.SpiUtils safeHandleClose
				ERROR: Close handler threw an exception
				java.util.concurrent.RejectedExecutionException: Task java.util.concurrent.FutureTask@681425 rejected from java.util.concurrent.ThreadPoolExecutor@102ae46[Shutting down, pool size = 1, active threads = 0, queued tasks = 0, completed tasks = 1]
					at java.util.concurrent.ThreadPoolExecutor$AbortPolicy.rejectedExecution(Unknown Source)
					at java.util.concurrent.ThreadPoolExecutor.reject(Unknown Source)
					at java.util.concurrent.ThreadPoolExecutor.execute(Unknown Source)
					at java.util.concurrent.AbstractExecutorService.submit(Unknown Source)
					at org.jboss.ejb.client.EJBClientContext.unregisterEJBReceiver(EJBClientContext.java:440)
					at org.jboss.ejb.client.EJBReceiverContext.close(EJBReceiverContext.java:57)
					at org.jboss.ejb.client.remoting.RemotingConnectionEJBReceiver$1$1.handleClose(RemotingConnectionEJBReceiver.java:185)
					at org.jboss.ejb.client.remoting.RemotingConnectionEJBReceiver$1$1.handleClose(RemotingConnectionEJBReceiver.java:182)
					at org.jboss.remoting3.spi.SpiUtils.safeHandleClose(SpiUtils.java:54)
					at org.jboss.remoting3.spi.AbstractHandleableCloseable$CloseHandlerTask.run(AbstractHandleableCloseable.java:501)
					at org.jboss.remoting3.spi.AbstractHandleableCloseable.runCloseTask(AbstractHandleableCloseable.java:406)
					at org.jboss.remoting3.spi.AbstractHandleableCloseable.closeComplete(AbstractHandleableCloseable.java:277)
					at org.jboss.remoting3.remote.RemoteConnectionChannel.closeAction(RemoteConnectionChannel.java:532)
					at org.jboss.remoting3.spi.AbstractHandleableCloseable.closeAsync(AbstractHandleableCloseable.java:359)
					at org.jboss.remoting3.remote.RemoteConnectionHandler.closeAllChannels(RemoteConnectionHandler.java:392)
					at org.jboss.remoting3.remote.RemoteConnectionHandler.sendCloseRequest(RemoteConnectionHandler.java:232)
					at org.jboss.remoting3.remote.RemoteConnectionHandler.closeAction(RemoteConnectionHandler.java:378)
					at org.jboss.remoting3.spi.AbstractHandleableCloseable.closeAsync(AbstractHandleableCloseable.java:359)
					at org.jboss.remoting3.ConnectionImpl.closeAction(ConnectionImpl.java:52)
					at org.jboss.remoting3.spi.AbstractHandleableCloseable.closeAsync(AbstractHandleableCloseable.java:359)
					at org.jboss.remoting3.EndpointImpl.closeAction(EndpointImpl.java:203)
					at org.jboss.remoting3.spi.AbstractHandleableCloseable.closeAsync(AbstractHandleableCloseable.java:359)
					at org.jboss.naming.remote.client.EndpointCache.release(EndpointCache.java:64)
					at org.jboss.naming.remote.client.EndpointCache$EndpointWrapper.closeAsync(EndpointCache.java:195)
					at org.jboss.naming.remote.client.InitialContextFactory$1.close(InitialContextFactory.java:231)
					at org.jboss.naming.remote.client.RemoteContext.finalize(RemoteContext.java:245)
					at java.lang.System$2.invokeFinalize(Unknown Source)
					at java.lang.ref.Finalizer.runFinalizer(Unknown Source)
					at java.lang.ref.Finalizer.access$100(Unknown Source)
					at java.lang.ref.Finalizer$FinalizerThread.run(Unknown Source)

				Out 17, 2014 4:03:00 PM org.jboss.ejb.client.remoting.ChannelAssociation$ResponseReceiver handleEnd
				INFO: EJBCLIENT000016: Channel Channel ID b3472920 (outbound) of Remoting connection 00e916a1 to localhost/127.0.0.1:8080 can no longer process messages
				*/
				send.close();
				sentsucessfull = true;
			} catch (Exception e) {
				System.out.println("Erro ao tentar enviar pela " + i
						+ "ª vez -> " + messageidentifier);
				//e.printStackTrace();
			}
		}
		return sentsucessfull;
	}

	/**
	 * Método que vai obter de um ficheiro o Log do WebCrawler. Este Log irá
	 * conter um Objecto Java com uma List<String>. Cada elemento String da
	 * Lista irá conter o xml resultante de uma execução do WebCrawler que
	 * falhou o envio deste xml para o WildFly.
	 * 
	 * @author Marco Pedrosa, Vasco Patrício
	 * @return ArrayList<String> com a Lista de mensagens falhadas (Strings com
	 *         o xml).
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<String> loadWebCrawlerLog() throws IOException,
			ClassNotFoundException, ClassCastException {
		ArrayList<String> result = new ArrayList<String>();
		InputStream file = new FileInputStream(log_WebCrawler_filename);
		InputStream buffer = new BufferedInputStream(file);
		ObjectInput input = new ObjectInputStream(buffer);
		try {
			// deserialize the List
			result = (ArrayList<String>) input.readObject();
		} finally {
			input.close();
			buffer.close();
			file.close();
		}
		return result;
	}

	/**
	 * Método que vai gravar para um ficheiro o Log do WebCrawler. Este Log irá
	 * conter um Objecto Java com uma List<String>. Cada elemento String da
	 * Lista irá conter o xml resultante de uma execução do WebCrawler que
	 * falhou o envio deste xml para o WildFly.
	 * 
	 * @author Marco Pedrosa, Vasco Patrício
	 * @param ArrayList
	 *            <String> com a Lista de mensagens falhadas (Strings com o
	 *            xml).
	 */
	public void saveWebCrawlerLog(ArrayList<String> oldmessages)
			throws IOException {
		OutputStream file = new FileOutputStream(log_WebCrawler_filename);
		OutputStream buffer = new BufferedOutputStream(file);
		ObjectOutput output = new ObjectOutputStream(buffer);
		try {
			// serialize the List and write to file
			output.writeObject(oldmessages);
		} finally {
			output.close();
			buffer.close();
			file.close();
		}
	}
}
