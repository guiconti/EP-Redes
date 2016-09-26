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

    //  Exibir a linha de requisição.
    System.out.println();
    //System.out.println(requestLine);

    // Obter e exibir as linhas de cabeçalho.
    String headerLine = null;
    while ((headerLine = br.readLine()).length() != 0) {
	    //System.out.println(headerLine);
    }

	// Extrair o nome do arquivo a linha de requisição.
	StringTokenizer tokens = new StringTokenizer(requestLine);

	// Pega o método da requisição (Post, GET, etc)
	String requestType = tokens.nextToken();

	// Pega diretório da requisição
	String fileName = tokens.nextToken();
	
	// Print do request
	System.out.println(requestType + " em " + fileName);

	// Acrescente um "." de modo que a requisição do arquivo esteja dentro do diretório atual.
	fileName = "." + fileName;

	// Abrir o arquivo requisitado.
	FileInputStream fis1 = null;
	boolean fileExists = true;
	try {
		fis1 = new FileInputStream(fileName);
	} catch (FileNotFoundException e) {
		fileExists = false;
	}

	String entityBody = null;

	if (fileExists) {

		// Envia o conteudo do arquivo requisitado
		sendBytes(fis1, os);

		// Enviar uma linha em branco para indicar o fim das linhas de cabeçalho.
		os.writeBytes(CRLF);

	} else {

		entityBody = "<HTML>" +
			"<HEAD><TITLE>404 Not Found</TITLE></HEAD>" +
			"<BODY>404 Not Found</BODY></HTML>";

		// Enviar a linha de conteudo.
		os.writeBytes(entityBody);

		// Enviar uma linha em branco para indicar o fim das linhas de cabeçalho.
		os.writeBytes(CRLF);
			
    }

    os.close();
    br.close();
    socket.close();

}

private static void sendBytes(FileInputStream fis, OutputStream os)
throws Exception {

	// Construir um buffer de 1K para comportar os bytes no caminho para o socket.
	byte[] buffer = new byte[1024];

    int bytes = 0;
    // Copiar o arquivo requisitado dentro da cadeia de saída do socket.
    while((bytes = fis.read(buffer)) != -1 ) {

        os.write(buffer, 0, bytes);

    }

}

private static String contentType(String fileName){

    if(fileName.endsWith(".htm") || fileName.endsWith(".html")) {
        return "text/html";
    }

    if(fileName.endsWith(".gif") || fileName.endsWith(".GIF")) {
        return "image/gif";
    }

    if(fileName.endsWith(".jpeg")) {
        return "image/jpeg";
    }

    return "application/octet-stream";

    }

}