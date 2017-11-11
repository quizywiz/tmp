package exchange.g3;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Random;
import java.util.HashMap;

import exchange.sim.Offer;
import exchange.sim.Request;
import exchange.sim.Sock;

public class SockCollection{

    ArrayList<Sock> collection;
    final double maxDist = 442.0;
    int id; // Player Id.

    int p; //pivot number
    double t; //threshold distance

    // Indicates the number of socks remaining to be exchanged.
    ArrayList<Sock> exchanges;

    Random rand;

    public SockCollection(List<Sock> socks, int id){
        this.id = id;
        this.collection = new ArrayList<>(socks);

        this.p = 0;

        rand = new Random();
        exchanges = new ArrayList<>();

        // Set threshold.
        setThreshold();

        preprocessSockCollection(true, true);
    }

    public void addSock(Sock newSock){
        collection.add(newSock);
    }

    public void removeSock(Sock oldSock){
        collection.remove(oldSock);
    }

    // TODO Convert the below if-else to an equation.
    private void setThreshold() {
        int n = collection.size();
        if (n >= 2000) {
            this.t = 14;
        }
        if (n >= 400) {
            this.t = 25;
        }
        else if (n >= 200) {
            this.t = 31;
        }
        else if (n >= 100) {
            this.t = 40;
        }
        else if (n >= 40) {
            this.t = 60;
        }
        else if (n >= 20) {
            this.t = 77;
        }
        else {
            this.t = 90;
        }
    }

    // Obtained Blossom code from team 1.
    private void pairBlossom(List<Sock> input) {
        int[] match = new Blossom(getCostMatrix(input), true).maxWeightMatching();
        ArrayList<Sock> result = new ArrayList<>();
        for (int i=0; i<match.length; i++) {
            if (match[i] < i) continue;
            result.add(input.get(i));
            result.add(input.get(match[i]));
        }

        input.clear();
        input.addAll(result);
    }

    private float[][] getCostMatrix(List<Sock> input) {
        int n = input.size();
        float[][] matrix = new float[n*(n-1)/2][3];
        int idx = 0;

        for (int i = 0; i < n; i++) {
            for (int j=i+1; j< n; j++) {
                matrix[idx] = new float[]{i, j, (float)(-input.get(i).distance(input.get(j)))};
                idx ++;
            }
        }

        return matrix;
    }

    // Change the order of socks in your collection using some algorithm.
    private void preprocessSockCollection(boolean rePair, boolean sort) {
        // Run Blossom.
        if (rePair) {
            // Check if collection size is large and we've already run Blossom once.
            if (collection.size() >= 2000 && this.p > 0) {
                List<Sock> badSocks = collection.subList(p, collection.size());
                pairBlossom(badSocks);
            } else {
                pairBlossom(collection);
            }
        }

        this.p = 0;

        // Check if we need to sort.
        if (!sort) {
            return;
        }

        for (int i = 0; i < collection.size(); i+=2) {
            if (collection.get(p).distance(collection.get(p+1)) > t) {
                Sock s1 = collection.get(p);
                Sock s2 = collection.get(p+1);
                collection.remove(p);
                collection.remove(p);
                collection.add(s1);
                collection.add(s2);
            } else {
                p += 2;
            }
        }
    }
    
    private Sock[] getWorstPairingSocks(List<Sock> recentlyDesiredSocks) {
        Sock s1;
        Sock s2;

        if (recentlyDesiredSocks.size() == 0) {
            // Get the worst set of socks from "p" position onwards.
            int w1 = -1;
            int w2 = -1;
            double maxDistance = -1.0;

            for (int i = p; i < collection.size(); i += 2) {
                Sock sock1 = collection.get(i);
                Sock sock2 = collection.get(i + 1);

                if (sock1.distance(sock2) > maxDistance) {
                    w1 = i;
                    w2 = i+1;
                    maxDistance = sock1.distance(sock2);
                }
            }

            s1 = collection.get(w1);
            s2 = collection.get(w2);
        }
        else {

            double thisSocksMinDistToDesirable;
            Double d;
            HashMap<Integer, Double> eachSocksMinDistToDesirable = new HashMap<Integer, Double>();
            for (int i = p; i < collection.size(); i++) {
                thisSocksMinDistToDesirable = maxDist;
                for (Sock s : recentlyDesiredSocks) {
                    d = s.distance(collection.get(i));
                    if (d < thisSocksMinDistToDesirable) {
                        thisSocksMinDistToDesirable = d;
                    }
                }
                eachSocksMinDistToDesirable.put(i, thisSocksMinDistToDesirable);
            }

            double closestDistance = maxDist;
            double secondClosestDistance = maxDist;
            int mostDesiredIndexOfOurBadSocks = -1;
            int secondMostDesiredIndexOfOurBadSocks = -1;
            for (Integer i : eachSocksMinDistToDesirable.keySet()) {
                d = eachSocksMinDistToDesirable.get(i);
                if (d < closestDistance) {
                    closestDistance = d;
                    mostDesiredIndexOfOurBadSocks = i;
                }
            }
            for (Integer i : eachSocksMinDistToDesirable.keySet()) {
                if (i != mostDesiredIndexOfOurBadSocks) {
                    d = eachSocksMinDistToDesirable.get(i);
                    if (d < secondClosestDistance) {
                        secondClosestDistance = d;
                        secondMostDesiredIndexOfOurBadSocks = i;
                    }
                }
            }

            s1 = collection.get(mostDesiredIndexOfOurBadSocks);
            s2 = collection.get(secondMostDesiredIndexOfOurBadSocks);
        }

        collection.remove(s1);
        collection.remove(s2);

        // Add them at the end.
        collection.add(s1);
        collection.add(s2);

        exchanges.add(s1);
        exchanges.add(s2);

        return new Sock[] { s1, s2 };
    }

    // Check if a new sock will give us a better pairing than an existing pair.
    private boolean checkNewSockCompatibility(Sock sock) {
        // Check if new sock pairs well with our unmatched sock list.
        // If better than threshold value, take it since it improves our score.
        // Ff worse, ignore it.

        for (int i = p; i < collection.size(); i += 2) {
            if (sock.distance(collection.get(i)) < t){
                return true;
            }
        }

        return false;
    }

    private void updateThreshold() {
        if (this.t <= 20) {
            this.t -= 5;
        } else {
            this.t -= 10;
        }
    }

    public Sock[] getWorstPairSocks(List<Sock> recentlyDesiredSocks){
        this.exchanges.clear();

        // Preprocess if our pivot doesn't separate the list well.
        if (p >= collection.size() - 4) {
            updateThreshold();
            preprocessSockCollection(false, true);
        }

        return getWorstPairingSocks(recentlyDesiredSocks);
    }

    public Sock getSock(int id) {
        return collection.get(id);
    }

    public void putSock(Sock oldSock, Sock s) {
        removeSock(oldSock);
        exchanges.remove(oldSock);

        double maxD = maxDist;
        int s2 = 0;
        for (int i = p; i < collection.size() - exchanges.size(); ++i) {
            // Find the match for the Sock s
            // Add into good array
            if (collection.get(i).distance(s) < maxD) {
                s2 = i;
                maxD = collection.get(i).distance(s);
            }
        }

        Sock sock2 = collection.get(s2);

        collection.remove(sock2);

        collection.add(p, sock2);
        collection.add(p, s);

        p += 2;
    }

    // Requests for the what we think is the best offer.
    public Request requestBestOffer(List<Offer> offers) {

        Offer myOffer = offers.get(id);

        // Removing socks from our collection and adding them back in later.
        int s1Index = collection.indexOf(myOffer.getFirst());
        collection.remove(myOffer.getFirst());
        int s2Index = collection.indexOf(myOffer.getSecond());
        collection.remove(myOffer.getSecond());

        List<Integer> toPickOffer = new ArrayList<>(Arrays.asList(-1, -1));
        List<Integer> toPickRank = new ArrayList<>(Arrays.asList(-1, -1));

        for (int i = 0; i < offers.size(); ++i) {
            if (toPickOffer.get(0) != -1 && toPickOffer.get(1) != -1) {
                // We have already selected socks.
                // We're not going to look at the rest of the offers.
                break;
            }

            // Skip our socks.
            if (i == id) {
                continue;
            }

            for (int j = 1; j < 3; ++j) {
                if (offers.get(i).getSock(j) == null) {
                    // I'm not sure what condition to check for if the player has not offered
                    //  a sock.
                    continue;
                }

                // Only need to compare it with p to the end
                if (checkNewSockCompatibility(offers.get(i).getSock(j))){
                    if (toPickOffer.get(0) == -1) {
                        toPickOffer.set(0, i);
                        toPickRank.set(0, j);
                    }
                    else if (toPickOffer.get(1) == -1) {
                        toPickOffer.set(1, i);
                        toPickRank.set(1, j);
                    }
                }
            }
        }

        collection.add(s2Index, myOffer.getSecond());
        collection.add(s1Index, myOffer.getFirst());

        return new Request(toPickOffer.get(0), toPickRank.get(0), toPickOffer.get(1), toPickRank.get(1));
    }

    public ArrayList<Sock> getCollection(boolean rePair) {
        // Re-run our pairing algorithm if requested for.
        if (rePair) {
            preprocessSockCollection(true, false);
        }

        return collection;
    }

    // Causing a disruption in our data.
    // TODO: Do something better!!!
    // Currently we are taking a sock in some random position and moving it to the end.
    // Boolean parameter to decide whether to shuffle or increase threshold.
    public void shuffle(boolean reorder) {
        if (reorder) {
            int pos = rand.nextInt(collection.size() - p) + p;
            Sock s1 = collection.get(pos);
            collection.remove(pos);
            collection.add(s1);
        } else {
            this.t += 5; //increase threshold by 5
            preprocessSockCollection(false, true);
        }

    }
}
