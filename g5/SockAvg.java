package exchange.g5;
import exchange.sim.Sock;

public class SockAvg{
	double avg_distance;
	Sock sock;

	public SockAvg(Sock sock, Sock[] socks){
		this.sock = sock;
		double temp = 0;
		for (int i=0; i<socks.length; i++){
			temp += sock.distance(socks[i]);
		}
		temp = temp/(socks.length-1);
		avg_distance = temp;
	}

	public String toString() {
        return "(" + sock + ": " + avg_distance + ")";
    }
}