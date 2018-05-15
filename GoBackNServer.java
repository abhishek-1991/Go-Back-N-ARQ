import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;

public class GoBackNServer implements Runnable {

	int port;
	String file;
	double prob;
	volatile boolean received;
	DatagramSocket sock;
	int ack;
	
	
	void startServer(int portnum,String filename,double probability)
	{
		port = portnum;
		file = filename;
		prob = probability;
		
		try
		{
			sock = new DatagramSocket(port);
			System.out.println("Server running at " + InetAddress.getLocalHost().getHostAddress()
            		+ " on port " + sock.getLocalPort());
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		received = true;
		ack = -1;
		//System.out.println("Thread Started");
		new Thread(this).run();
		//System.out.println("Thread Started");
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		try
		{
			//System.out.println("Thread Started");
			int lostcount = 0;
			int size = 1024*1000*2;
			
			File fil = new File(file);
			FileOutputStream fout = new FileOutputStream(fil);
			BufferedOutputStream buff = new BufferedOutputStream(fout);
			
			while(received)
			{
				//System.out.println("DEBUG");
				byte datbuff[]=new byte[size];
				int len = datbuff.length;
				DatagramPacket packet = new DatagramPacket(datbuff, len);
				//System.out.println("DEBUG");
				sock.receive(packet);
				//System.out.println("DEBUG");
				ByteArrayInputStream byteip = new ByteArrayInputStream(packet.getData());
				ObjectInputStream objip = new ObjectInputStream(byteip);
				Packet pack = (Packet) objip.readObject();
				if(pack == null)
				{
					buff.close();
					sock.close();
					System.out.println("Packet Lost = " + lostcount);
					break;
				}
				
				Random rand = new Random();
				double randprob = (double)rand.nextInt(100)/100;
				int checksum = cksum(pack.getData());
				int seq = pack.getSequence();
				
				//System.out.println("DEBUG");
				
				if(seq != ack)
				{
					//System.out.println("DEBUG");
					lostcount = calcProb(buff,lostcount,21845,packet,pack,randprob,checksum,seq);
				}
				else
				{
					//System.out.println("DEBUG");
					byte ackdat[]=null;
					short cksum =0;
					short acktyp = (short)43690;
					Packet ackpack =new Packet(seq,cksum,acktyp,ackdat);
					ByteArrayOutputStream byteout = new ByteArrayOutputStream();
					ObjectOutputStream os = new ObjectOutputStream(byteout);
					os.writeObject(ackpack);
					byte[] acknowledgeData = byteout.toByteArray();
					int acklen = acknowledgeData.length;
					DatagramPacket ackPacket = new DatagramPacket(acknowledgeData, acklen, packet.getAddress(), packet.getPort());
					sock.send(ackPacket);
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			received = false;
		}

	}
	
	int calcProb(BufferedOutputStream buff, int lostcount, int typeConst, DatagramPacket packet,
				 Packet pack, double randprob, int checksum, int seq)
	{
		try 
		{
			if(prob < randprob)
			{
				if(pack.getType() == typeConst)
				{
					if(pack.getChecksum() == checksum)
					{
						ack=ack+1;
						buff.write(pack.getData());
						buff.flush();
						//sendAck(seq,packet);
						Packet ackmnt = new Packet(seq, 0, (short)43690, null);
						ByteArrayOutputStream byteop = new ByteArrayOutputStream();
						ObjectOutputStream outstr = new ObjectOutputStream(byteop);
						outstr.writeObject(ackmnt);
						byte[] ackdat = byteop.toByteArray();
						DatagramPacket ackpack = new DatagramPacket(ackdat, ackdat.length,packet.getAddress(),packet.getPort());
						sock.send(ackpack);
					}
					else
					{
						System.out.println("Checksum for sequence number: "+seq+" is incorrect");
					}
				}
			}
			else
			{
				System.out.println("Packet lost, sequence number = "+seq);
				lostcount = lostcount+1;
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return lostcount;
	}
	
	int cksum(byte[] data)
	{
		int checksum = 0;
		if(data != null)
		{
			try
			{
				for(int i = 0; i < data.length; i++) 
				{
					if(i%2 == 0)
						checksum += ((data[i] << 8) & 0xFF00);
					else
						checksum += ((data[i]) & 0xFF);

		        	if((data.length % 2) != 0)
		        	{
		        		checksum +=  0xFF;
		        	}
		
		            while ((checksum >> 16) == 1)
		            {
		            	 checksum =  ((checksum & 0xFFFF) + (checksum >> 16));
		                 checksum =  ~checksum;
		            }
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		
		return checksum;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		if(args.length != 3)
		{
			System.out.println("Error: Execute as java GoBackNServer <port#> <file-name> <probability>");
			System.exit(-1);
		}
		//System.out.println(args[0]+" "+args[1]+" "+args[2]);
		new GoBackNServer().startServer(Integer.parseInt(args[0]), args[1], Double.parseDouble(args[2]));
	}

}