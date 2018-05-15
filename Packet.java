
import java.io.*;
import java.net.*;
import java.util.Random;
import static java.net.InetAddress.getByName;

class Packet implements Serializable
{

    private static final long serialVersionUID = 1L;
    public long time_sent;
    public int seq_no;
    public int check_sum;
    public int packet_type;
    public byte content[];
    
    public Packet(int p_seq_no, int p_check_sum, int p_packet_type, byte[] p_content) {
        seq_no = p_seq_no;
        check_sum = p_check_sum;
        packet_type = p_packet_type;
        content = p_content;
    }

    public byte[] getData()
    {
    	return content;
    }
    
    public int getSequence()
    {
    	return seq_no;
    }
    
    public int getChecksum()
    {
    	return check_sum;
    }
    
    public int getType()
    {
    	return packet_type;
    }
}



