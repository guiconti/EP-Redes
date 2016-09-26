import java.io.* ;
import java.net.* ;
import java.util.* ;

public final class WebServer{ 

	public static void main(String argv[]) throws Exception {
        
        // Numero da porta em que o nosso servidor vai rodar.
		int port = 6789;

        //Estabelecer o socket de escuta.
		ServerSocket listenSocket = new ServerSocket(6789);

		//Print no console para indicar que o servidor iniciou
		System.out.println("Servidor esta online e operando na porta: " + port);

        //Process HTTP service requests in an infinite loop
		while (true) {

			//Escutar a requisicao de conexao TCP
			Socket connectionSocket = listenSocket.accept();

            //Construir um objeto para processar a mensagem de requisição HTTP.
            HttpRequest request = new HttpRequest (connectionSocket);

            // Criar um novo thread para processar a requisição.
            Thread thread = new Thread(request);
            
            //Iniciar o thread.
            thread.start();

		}
	}
}

final class HttpRequest implements Runnable {

	final static String CRLF = "\r\n";
	Socket socket;

	// Construtor
	public HttpRequest(Socket socket) throws Exception{

		this.socket = socket;

	}

	// Implemente o método run() da interface Runnable.
	public void run() {

		try {
		processRequest();
        } catch (Exception e) {
            System.out.println(e);
        }

	}

	private void processRequest() throws Exception {

	// Obter uma referência para os trechos de entrada e saída do socket.
	InputStream is = socket.getInputStream();
	DataOutputStream os = new DataOutputStream(socket.getOutputStream());

	// Ajustar os filtros do trecho de entrada.
	FilterInputStream fis;
	BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	
    // Obter a linha de requisição da mensagem de requisição HTTP.
    String requestLine = br.readLine();

    // Obter e exibir as linhas de cabeçalho.
    String headerLine = null;

	// Pega a data atual
	Date actualDate = new Date();

	//PrintWriter out = new PrintWriter("log.txt");
	BufferedWriter out = new BufferedWriter(new FileWriter("log.txt", true));

    while ((headerLine = br.readLine()).length() != 0) {

		try {

			out.write(headerLine + "\n");

		} catch (Exception e) {

			System.out.println("Write Exception");

		}
    }

	// Armazena no log a data da requisição
	out.write("Data: " + actualDate + "\n");

	// Extrair o nome do arquivo a linha de requisição.
	StringTokenizer tokens = new StringTokenizer(requestLine);

	// Pega o método da requisição (Post, GET, etc)
	String requestType = tokens.nextToken();

	// Pega diretório da requisição
	String fileName = tokens.nextToken();
	
	// Print do request
	System.out.println(requestType + " em " + fileName);

	// Verifica se é um dirétório
	if (contentType(fileName) == "directory") {

		try {

			// Abro o arquivo
			BufferedReader brConfig = new BufferedReader(new FileReader("config.txt"));

			// Leio a primeira linha do arquivo
			String configLine = brConfig.readLine();

			// Tokenizer para dividir a string caso haja mais caracteres
			StringTokenizer configTokens = new StringTokenizer(configLine);

			// Pego a config
			String config = configTokens.nextToken();

			// Fecha arquivo de leitura
			brConfig.close();

			if (config.equals("1")){

				// Caso 1
				File folder = new File("." + fileName);
				File[] listOfFiles = folder.listFiles();

				String contentHtml = "<html>\n<body>";

				for (int i = 0; i < listOfFiles.length; i++) {

					if (listOfFiles[i].isFile()) {

						contentHtml = contentHtml + "\n" + "<br/>" + "File " + listOfFiles[i].getName();

						//System.out.println("File " + listOfFiles[i].getName());

					} else if (listOfFiles[i].isDirectory()) {

						contentHtml = contentHtml + "\n" + "<br/>" + "Directory: " + "Directory " + listOfFiles[i].getName(); 
						//System.out.println("Directory " + listOfFiles[i].getName());

					}

				}

				contentHtml = contentHtml + "\n" + "</body>" + "\n" + "</html>";

				BufferedWriter outHtml = new BufferedWriter(new FileWriter("directory.html", false));
				outHtml.write(contentHtml);
				outHtml.close();

				fileName = "/directory.html";

			} else if (config.equals("2")) {

				// Caso 2 exibimos uma mensagem de listagem de diretório
				// Não disponível
				fileName = "/forbidden.html";
				
			} else {

				// Caso 3 exibimos um index.html
				fileName = "/index.html";

			}

		} catch (Exception e ){

			System.out.println("Error reading config file.");

		}

	}

	// Acrescente um "." de modo que a requisição do arquivo esteja dentro do diretório atual.
	fileName = "." + fileName;

	// Armazena no log o arquivo requisitado
	out.write("Arquivo requisitado: " + fileName + "\n");

	// Abrir o arquivo requisitado.
	FileInputStream fis1 = null;
	boolean fileExists = true;

	try {
		fis1 = new FileInputStream(fileName);
	} catch (FileNotFoundException e) {
		fis1 = new FileInputStream("./notfound.html");
		fileExists = false;
	}

	String entityBody = null;

	if (fileExists) {

		// Envia o conteúdo do arquivo requisitado
		int bytesSent = sendBytes(fis1, os);

		// Escreve o tamanho do conteúdo enviado para o usuário no log
		out.write("Total de bytes enviados: " + bytesSent + "\n\n");
		out.close();

		// Enviar uma linha em branco para indicar o fim das linhas de cabeçalho.
		os.writeBytes(CRLF);

	} else {

		// Envia o conteúdo do arquivo requisitado
		int bytesSent = sendBytes(fis1, os);

		// Escreve o tamanho do conteúdo enviado para o usuário no log
		out.write("Total de bytes enviados: " + bytesSent + "\n\n");
		out.close();

		// Enviar uma linha em branco para indicar o fim das linhas de cabeçalho.
		os.writeBytes(CRLF);
			
    }

    os.close();
    br.close();
    socket.close();

}

private static int sendBytes(FileInputStream fis, OutputStream os)
throws Exception {

	// Construir um buffer de 1K para comportar os bytes no caminho para o socket.
	byte[] buffer = new byte[1024];

    int bytes = 0;
	int bytesLogSum = 0;
    // Copiar o arquivo requisitado dentro da cadeia de saída do socket.
    while((bytes = fis.read(buffer)) != -1 ) {

        os.write(buffer, 0, bytes);

		// Somamos o total de bytes de cada buffer para utilizarmos no nosso log
		bytesLogSum += bytes;

    }

	return bytesLogSum;

}

private static String contentType(String fileName){

	if(!fileName.contains(".")) {
        return "directory";
    }

    if(fileName.endsWith(".htm") || fileName.endsWith(".html")) {
        return "text/html";
    }

    if(fileName.endsWith(".gif") || fileName.endsWith(".GIF")) {
        return "image/gif";
    }

    if(fileName.endsWith(".jpeg")) {
        return "image/jpeg";
    }

	if(fileName.endsWith(".txt")) {
        return "text/html";
    }

    return "application/octet-stream";

    }

}