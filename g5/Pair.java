package exchange.g5;
import exchange.sim.Sock;

public class Pair{
	double distance;
	Sock left;
	Sock right;

	public Pair(Sock one, Sock two){
		left = one;
		right = two;
		distance = one.distance(two);
	}

	public void recalcDistance(){
		distance = left.distance(right);
	}

	public String toString() {
        return "(" + left + ", " + right + ")";
    }
}