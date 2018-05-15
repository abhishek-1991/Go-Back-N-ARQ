
import java.io.*;
import java.net.*;
import java.util.Random;
import static java.net.InetAddress.getByName;


public class Client extends Thread{
   
	private DatagramSocket dgram_socket = null;
    private int timeout = 1000;
    private String server;
    private int server_port;
    private int no_of_packets;
    private int last_packet;
    private String fn;
    private int size_of_window;
    private int trans_seq  = -1;
    private volatile int ack_seq  = -1;
    private int mss;
    private final Packet client_buffer[];

    public Client(String server, int server_port, String fn, int size_of_window, int MSS) throws SocketException, UnknownHostException {
	   
    		perform_init(server, server_port, fn, size_of_window, MSS);
	    	client_buffer = new Packet[this.size_of_window];
    }

	
    

    
    private class get_remaining_packets extends Thread{
        private final DatagramSocket contentgramSock;
        private boolean flag;
        public get_remaining_packets(DatagramSocket contentsocket) {
            this.contentgramSock = contentsocket;
            flag = true;
        }

        public void done()
        {
            flag = false;
        }

        public void run(){
            while(flag)
            {
                byte content_client_buffer[] = new byte[1024 * 1000 * 2];
                DatagramPacket contentgrampacket = new DatagramPacket(content_client_buffer, content_client_buffer.length);

                try {
              
                    if(!contentgramSock.isClosed()) 
                    {
                    		contentgramSock.receive(contentgrampacket);
                        ObjectInputStream input_stream = new ObjectInputStream(new ByteArrayInputStream(contentgrampacket.getData()));
                        Packet packet = (Packet) input_stream.readObject();
                        if (packet.packet_type == (short) 43690)
                        	ack_seq = packet.seq_no;
                    }
                } catch (IOException | ClassNotFoundException e) {
                    System.out.println("No remaining packets");
                }
            }
        }
    }

    public void run(){
        try{
        	int run_flag = 1;
        	if(run_flag == 1) 
        	{
	            long start_time = System.currentTimeMillis();
	            File file = new File(fn);
	            
	            if(file.exists())
	            {
	            		get_remaining_packets next_packet = null;
	            		int size = (int) file.length();
	  	            no_of_packets = size/mss;
	  	            last_packet = size % mss;
	  	            byte content[] = new byte[mss];
	  	            
	  	            int no_of_pkts_lost = 0;
	  	            int still_next = 1;
	  	            FileInputStream file_input_stream = new FileInputStream(file);
	  	            while(file_input_stream.read(content) > -1)
	  	            {
	  	                no_of_pkts_lost = get_no_lost_packets(no_of_pkts_lost);
		  	            byte content_first[] = new byte[mss];
		  	            short contentType = (short) 21845;
		  	            	byte content_last[] = new byte[last_packet];
		  	            content_first = content;
		  	            trans_seq++;
		  	            if(trans_seq == no_of_packets) {
		  	            	for(int k = 0 ; k < last_packet ; k++){
		  	            		content_last[k] = content_first[k];
		  	            	}
		  	            	content_first = null;
		  	            	content_first = content_last;
		  	            }
		  	            int check_sum = check_sum(content_first);
		  	            Packet packet = new Packet(trans_seq, check_sum, contentType, content_first);
		  	            int index = (trans_seq % size_of_window);
		  	            client_buffer[index] = packet;
		  	            send_packet(packet);
	  	                if(still_next == 1)
	  	                {
	  	                    still_next = 0;
	  	                    next_packet = new get_remaining_packets(dgram_socket);
	  	                    next_packet.start();
	  	                }
	  	                
	  	                int tempAcked = ack_seq;
	                  	no_of_pkts_lost = get_packets_lost(no_of_pkts_lost, tempAcked);
	  	            }
			  	        
			  	        file_input_stream.close();
			  			long end_time = System.currentTimeMillis();	
			  			send_packet(null);
			  			next_packet.done();
			  			run_flag = 0;
			  			dgram_socket.close();
			  			print_output(start_time, no_of_pkts_lost, end_time);	            
			  			dgram_socket.close();
	            } 
	            
	            else
	            {
	            		System.out.println("File cannot be found");
	            		
	            }
        	}
        } catch (IOException e) {
           System.out.println("The server can't be reached");
        }
    }

	private void print_output(long start_time, int no_of_pkts_lost, long end_time) {
		System.out.println("The delay occurred " + (end_time - start_time));
		System.out.println("Number of Packets lost: " + no_of_pkts_lost);
	}

	private int get_packets_lost(int no_of_pkts_lost, int tempAcked) throws IOException {
		boolean success = false;
		while(tempAcked != trans_seq) {
		    if(System.currentTimeMillis() - client_buffer[(tempAcked+1) % size_of_window].time_sent <= timeout){
		    	success = true;
		    }
		    else {
		    	success = false;
		    }
			if(!success) {
		        int j = 0;
		        while(j < (trans_seq - tempAcked)) {
		            System.out.println("Timeout, sequence number = " + client_buffer[(tempAcked + 1 + j) % size_of_window].seq_no);
		            no_of_pkts_lost++;
		            send_packet(client_buffer[(tempAcked + 1 + j) % size_of_window]);
		            j++;
		        }
		    }
		    tempAcked = ack_seq;
		}
		return no_of_pkts_lost;
	}

	private int get_no_lost_packets(int no_of_pkts_lost) throws IOException {
		while(trans_seq - ack_seq == size_of_window) 
		{
		    if(System.currentTimeMillis() - client_buffer[(ack_seq+1) % size_of_window].time_sent > timeout)
		    {
		        int temp = ack_seq;
		        int i = 0;
		        while(i < (trans_seq - ack_seq)) 
		        {
		            System.out.println("Timeout, sequence number = " + client_buffer[(temp + 1 + i) % size_of_window].seq_no);
		            no_of_pkts_lost++;
		            send_packet(client_buffer[(temp+1+i) % size_of_window]);
		            i++;
		        }
		    }
		}
		return no_of_pkts_lost;
	}

  
    private void send_packet(Packet packet) throws IOException {
        ByteArrayOutputStream byte_output_stream = new ByteArrayOutputStream();
        ObjectOutputStream output_stream = new ObjectOutputStream(byte_output_stream);
        output_stream.writeObject(packet);
        byte[] sendData = byte_output_stream.toByteArray();
        DatagramPacket send_packet = new DatagramPacket(sendData, sendData.length,getByName(server), server_port);
        if(packet == null)
        {
        		System.out.println("No data - Empty Packet");
        	}
        else
        	{
        		packet.time_sent = System.currentTimeMillis();
        	}
        	
        dgram_socket.send(send_packet);
    }

    private static int check_sum(byte content[]) {
        int ch_sum_val = 0;
        if (content == null) 
        	{
        		return 0;
        	}
        try 
        {
	        	int i = 0;
	        	while(i < content.length)
	        	{
	            	ch_sum_val = ((i % 2) == 0) ? (ch_sum_val + ((content[i] << 8) & 0xFF00)) : (ch_sum_val + ((content[i]) & 0xFF));
	
		        	if((content.length % 2) != 0){
		        		ch_sum_val = ch_sum_val + 0xFF;
		        	}
		
		            while ((ch_sum_val >> 16) == 1){
		            	 ch_sum_val =  ((ch_sum_val & 0xFFFF) + (ch_sum_val >> 16));
		                 ch_sum_val =  ~ch_sum_val;
		            }
		            i++;
	        	} 
        }
        catch (Exception e){
        		e.printStackTrace();
        }
        return ch_sum_val;
    }

    public static void main(String[] args) throws InterruptedException {
    	if(args.length != 5 ) return;
    	try{
    		String server = args[0];
    		int server_port = Integer.parseInt(args[1]);
    		String fn = args[2];
    		int size_of_window = Integer.parseInt(args[3]);
    		int max_seg_size = Integer.parseInt(args[4]);
    		new Client(server, server_port, fn, size_of_window, max_seg_size).start();
    	} catch (UnknownHostException | SocketException e) {
    		System.out.println("There is an exception");
    	}
    }

    private void perform_init(String Server, int Server_port, String Fn, int Size_of_window, int MSS) throws SocketException, UnknownHostException {
		
		server = Server;
	    	server_port = Server_port;
	    	fn = Fn;
	    	size_of_window = Size_of_window;
	    	mss = MSS;
	    	Random random = new Random();
	    	int portNum = random.nextInt(5000) + 1000;	
	    	dgram_socket = new DatagramSocket(portNum);
	    	dgram_socket.connect(getByName(server), server_port);
	    	System.out.println("Client address is " + InetAddress.getLocalHost().getHostAddress());
	    	System.out.println("Client port is " + dgram_socket.getLocalPort());
	    	System.out.println("Window size is "+ size_of_window);
	    	System.out.println("Maximum segment size is "+ mss);
	}
    
    

}