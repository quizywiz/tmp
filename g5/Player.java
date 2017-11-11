package exchange.g5;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Comparator;
import java.util.Collections;
import java.lang.Math;

import exchange.sim.Offer;
import exchange.sim.Request;
import exchange.sim.Sock;
import exchange.sim.Transaction;
import exchange.g5.Pair;
import exchange.g5.SockAvg;

public class Player extends exchange.sim.Player {
    /*
        Inherited from exchange.sim.Player:
        Random random   -       Random number generator, if you need it
        Remark: you have to manually adjust the order of socks, to minimize the total embarrassment
                the score is calculated based on your returned list of getSocks(). Simulator will pair up socks 0-1, 2-3, 4-5, etc.
     */
    private int id1, id2, id3, id, n, t, threshold;
    private int turn = 0;
    private Sock[] socks;
    private ArrayList<Sock> socks_sorted =  new ArrayList<>();
    private ArrayList<Pair> sock_pairs = new ArrayList<>();
    private ArrayList<Pair> paired = new ArrayList<>();
    private ArrayList<Pair> pairedAll = new ArrayList<>();
    private ArrayList<SockAvg> socks_avg = new ArrayList<>();
    private Sock[] socks_score;
    boolean traded = false;
    int tradeCount = 0;
    private ArrayList<Sock> sorted_list = new ArrayList<>();
    int unpairedIndex = 0;
    int tradeSucceed = 0;
    Sock center;
    ArrayList<PriorOffer> priorOffers1 = new ArrayList<>();
    ArrayList<PriorOffer> priorOffers2 = new ArrayList<>();
    private boolean switchSock = false;
    ArrayList<Integer> pid2 = new ArrayList<>();

    private class PriorOffer{
        public int pid;
        public int sockNum;

        public PriorOffer(int pid, int num){
            this.pid = pid;
            sockNum = num;
        }
    }

    @Override
    public void init(int id, int n, int p, int t, List<Sock> socks) {
        this.id = id;
        this.n = n; this.t = t; this.threshold = (int) (t*0.9);
        this.socks = (Sock[]) socks.toArray(new Sock[2 * n]);
        this.socks_score = new Sock[n*2];
        
        sorted_list = sortSocks(this.socks);
        sorted_list = sortGreedy(sorted_list);
        
        int topTenPerc = Math.max((int)(n*0.1), 1);
        
        removeFromPool(topTenPerc);
        buildSockArray(paired, sorted_list);
        id1 = sorted_list.size()-1;
        id2 = sorted_list.size()-2;
        center = calcCenter(sorted_list);
        

    }

    @Override
    public Offer makeOffer(List<Request> lastRequests, List<Transaction> lastTransactions) {
        /*
			lastRequests.get(i)		-		Player i's request last round
			lastTransactions		-		All completed transactions last round.
		 */
        priorOffers1.clear(); priorOffers2.clear();
        for (int i=0; i<lastRequests.size(); i++){
            if (turn==0 || turn==threshold)
                break;
            if (i==id)
                continue;
            Request req = lastRequests.get(i);
            if (req.getFirstID() == id){
                if (req.getFirstRank()==1)
                    priorOffers1.add(new PriorOffer(i, 1));
                else
                    priorOffers2.add(new PriorOffer(i, 1));
            }
            if (req.getSecondID() == id){
                if (req.getSecondRank()==1)
                    priorOffers1.add(new PriorOffer(i, 2));
                else
                    priorOffers2.add(new PriorOffer(i, 2));
            }
        }

        if (turn < threshold){    
            if (tradeCount%2==0 && !traded){
                if (tradeCount!=0){
                    id1 -= 2;
                    id2 -= 2;    
                }
                if (id1 < 0 || id2 < 0)
                    resetOffer();
            }
            if (traded){
                traded = false;
                tradeCount = 0;
                initSockAvg();
                sorted_list = resortSocks(sorted_list);
            }
            if (tradeSucceed%3==0 && tradeSucceed!=0){
                if (sorted_list.size()/2 > 0.33*n){
                    int remove = (int)(sorted_list.size()*0.10);
                    removeFromPool(Math.max(remove, 1)); 
                    id1-=remove*2+1; id2-=remove*2+1;
                    if (id1 <0 || id2 < 0)
                        resetOffer();
                }
                sorted_list = resortSocks(sorted_list);
                buildSockArray(paired, sorted_list);
            }
            tradeCount++;
            return new Offer(sorted_list.get(id1), sorted_list.get(id2));
        }
        
        else{
            if (turn==threshold){
                pairUp();
                id3 = pairedAll.size()-1;
            }

            if (tradeCount%2==0){
                if (!switchSock)
                    switchSock = true;
                else{
                    switchSock = false;
                    id3-=2;
                    if (id3<=0)
                        resetOffer();
                }
            }
            tradeCount++;
            if (switchSock)
                return new Offer(pairedAll.get(id3).right, pairedAll.get(id3-1).right);
            else
                return new Offer(pairedAll.get(id3).left, pairedAll.get(id3-1).left);
        }
    }

    private void resetOffer(){
        this.tradeCount = 0;
        this.traded = false;
        if (turn < threshold){
            this.id1 = sorted_list.size()-1;
            this.id2 = sorted_list.size()-2;        
        }
        else{
            this.id3 = pairedAll.size()-1;
        }
        
    }

    @Override
    public Request requestExchange(List<Offer> offers) {
		/*
			offers.get(i)			-		Player i's offer
			For each offer:
			offer.getSock(rank = 1, 2)		-		get rank's offer
			offer.getFirst()				-		equivalent to offer.getSock(1)
			offer.getSecond()				-		equivalent to offer.getSock(2)
			Remark: For Request object, rank ranges between 1 and 2
		 */
        int firstID = -1; int secondID = -1;
        int firstRank = -1; int secondRank = -1;
        if (turn < threshold){
            double firstDistance = sorted_list.get(id1).distance(center);
            double secondDistance = sorted_list.get(id2).distance(center);
            if (turn%2==0){
                for (int i=0; i<offers.size(); i++){
                    if (i==id)
                        continue;
                    Offer offer = offers.get(i);
                    Sock first = offer.getFirst();
                    Sock second = offer.getSecond();
                   
                    if (first!=null && first.distance(center) < firstDistance){
                        firstID = i; firstRank = 1;
                        firstDistance = first.distance(center);
                    }
                    if (second!=null && second.distance(center) < secondDistance){
                        secondID = i; secondRank = 2;
                        secondDistance = second.distance(center);
                    }
                }    
            }
            else{
                for (int i=0; i<priorOffers1.size(); i++){
                    int p = priorOffers1.get(i).pid;
                    Offer offer = offers.get(p);
                    Sock temp;
                    if (priorOffers1.get(i).sockNum == 1)
                        temp = offer.getFirst();
                    else
                        temp = offer.getSecond();
                    if (temp!=null && temp.distance(center) < firstDistance){
                        firstID = p; firstRank = priorOffers1.get(i).sockNum;
                        firstDistance = temp.distance(center);
                    }
                }

                for (int i=0; i<priorOffers2.size(); i++){
                    int p = priorOffers2.get(i).pid;
                    Offer offer = offers.get(p);
                    Sock temp;
                    if (priorOffers2.get(i).sockNum == 1)
                        temp = offer.getFirst();
                    else
                        temp = offer.getSecond();
                    if (temp!=null && temp.distance(center) < secondDistance){
                        secondID = p; secondRank = priorOffers2.get(i).sockNum;
                        secondDistance = temp.distance(center);
                    }
                }    
            }
        }

        else{ //after threshold
            if (turn==threshold){
                turn++;
                return new Request(-1, -1, -1, -1); 
            }
            double firstDistance = pairedAll.get(id3).distance;
            double secondDistance = pairedAll.get(id3-1).distance;
            if (turn%2==0){
                for (int i=0; i<offers.size(); i++){
                    if (i==id)
                        continue;
                    Offer offer = offers.get(i);
                    Sock first = offer.getFirst();
                    Sock second = offer.getSecond();
                    if (first==null && second==null)
                        continue;
                    double fd1, sd1, fd2, sd2; 
                    fd1 = fd2 = firstDistance;
                    sd1 = sd2 = secondDistance;
                    if (!switchSock){
                        if (first!=null){
                            fd1 = first.distance(pairedAll.get(id3).right);
                            fd2 = first.distance(pairedAll.get(id3-1).right);
                        }
                        if (second!=null){
                            sd1 = second.distance(pairedAll.get(id3).right);
                            sd2 = second.distance(pairedAll.get(id3-1).right);
                        }    
                    }
                    else{
                        if (first!=null){
                            fd1 = first.distance(pairedAll.get(id3).left);
                            fd2 = first.distance(pairedAll.get(id3-1).left);    
                        }
                        if (second!=null){
                            sd1 = second.distance(pairedAll.get(id3).left);
                            sd2 = second.distance(pairedAll.get(id3-1).left);    
                        }    
                    }
                    if (fd1 < firstDistance){
                        firstID = i; firstRank = 1;
                        firstDistance = fd1;
                    }
                    if (sd1 < firstDistance){
                        firstID = i; firstRank = 2;
                        firstDistance = sd1;
                    }

                    if (fd2 < secondDistance){
                        secondID = i; secondRank = 1;
                        secondDistance = fd2;
                    }
                    if (sd2 < secondDistance){
                        secondID = i; secondRank = 2;
                        secondDistance = sd2;
                    }
                    
                }
            }

            else{
                for (int i=0; i<priorOffers1.size(); i++){
                    int p = priorOffers1.get(i).pid; 
                    Offer offer = offers.get(p);
                    Sock temp;
                    if (priorOffers1.get(i).sockNum == 1)
                        temp = offer.getFirst();
                    else
                        temp = offer.getSecond();
                    double d;
                    if (!switchSock)
                        d = temp.distance(pairedAll.get(id3).right);
                    else
                        d = temp.distance(pairedAll.get(id3).left);
                    if (temp!=null && d < firstDistance){
                        firstID = p; firstRank = priorOffers1.get(i).sockNum;
                        firstDistance = d;
                    }
                }

                for (int i=0; i<priorOffers2.size(); i++){
                    int p = priorOffers2.get(i).pid; 
                    Offer offer = offers.get(p);
                    Sock temp;
                    if (priorOffers2.get(i).sockNum == 1)
                        temp = offer.getFirst();
                    else
                        temp = offer.getSecond();
                     double d;
                    if (!switchSock)
                        d = temp.distance(pairedAll.get(id3-1).right);
                    else
                        d = temp.distance(pairedAll.get(id3-1).left);
                    if (temp!=null && d < secondDistance){
                        secondID = p; secondRank = priorOffers2.get(i).sockNum;
                        secondDistance = d;
                    }
                }
            }   
        }
        turn++;
        return new Request(firstID, firstRank, secondID, secondRank);        
    }

    @Override
    public void completeTransaction(Transaction transaction) {
        /*
            transaction.getFirstID()        -       first player ID of the transaction
            transaction.getSecondID()       -       Similar as above
            transaction.getFirstRank()      -       Rank of the socks for first player
            transaction.getSecondRank()     -       Similar as above
            transaction.getFirstSock()      -       Sock offered by the first player
            transaction.getSecondSock()     -       Similar as above
            Remark: rank ranges between 1 and 2
         */
        int rank;
        Sock newSock;
        if (transaction.getFirstID() == id) {
            rank = transaction.getFirstRank();
            newSock = transaction.getSecondSock();
        } else {
            rank = transaction.getSecondRank();
            newSock = transaction.getFirstSock();
        }
        if (turn < threshold){
            if (rank == 1) sorted_list.set(id1, newSock);
            else sorted_list.set(id2, newSock);
            traded = true;
            buildSockArray(paired, sorted_list);
            tradeSucceed++;    
        }

        else{
            if (rank==1){
                if (!switchSock)
                    pairedAll.get(id3).left = newSock;
                else
                    pairedAll.get(id3).right = newSock;
                pairedAll.get(id3).recalcDistance();
            }
            else{
                if (!switchSock)
                    pairedAll.get(id3-1).left = newSock;
                else
                    pairedAll.get(id3-1).right = newSock;
                pairedAll.get(id3-1).recalcDistance();
            }
            traded = true;
            tradeSucceed++;
        }
    }

    @Override
    public List<Sock> getSocks() {
        if (turn == t || turn == t-1){
            ArrayList<Sock> temp = convertPairs(pairedAll);
            blossomPair(temp);
            return temp;
        }
        if (turn < threshold)
            return Arrays.asList(socks);

        return convertPairs(pairedAll);
    }
    
    public void printSocks(){
        System.out.print("[");
        for (int i=0; i<socks.length; i++){
            System.out.print(socks[i]);
            if (i<socks.length-1)
                System.out.print(", ");
        }
        System.out.println("]");
    }

    public ArrayList<Sock> clusterSocks(ArrayList<Sock> unsorted_socks){
        ArrayList<Sock> sorted = unsorted_socks;
        Collections.sort(sorted, new Comparator<Sock>(){
            public int compare(Sock one, Sock other){
                return (one.R + one.G + one.B) - ((other.R + other.G + other.B));
            }
        }) ;
        return sorted;
    }

    public double calcScore(Sock[] socks){
        double total = 0;
        for (int i=0; i<socks.length; i+=2){
            total += socks[i].distance(socks[i+1]);
        }
        return total;
    }

    public ArrayList<Pair> sortPairs(ArrayList<Pair> unsorted_pairs){
        ArrayList<Pair> sorted = unsorted_pairs;
        Collections.sort(sorted, new Comparator<Pair>(){
            public int compare(Pair one, Pair two){
                return (one.distance < two.distance) ? -1 : 1;
            }
        });
        return sorted;
    }

    //public ArrayList<SockAvg>

    public Sock[] buildSockArrayPair(ArrayList<Pair> pairs){
        Sock[] array = new Sock[n*2];
        for (int i=0; i<pairs.size(); i++){
            array[i*2] = pairs.get(i).left;
            array[i*2+1] = pairs.get(i).right;
        }
        return array;
    }

    public Sock[] buildSockArrayAvg(ArrayList<SockAvg> avg){
        Sock[] array = new Sock[n*2];
        for (int i=0; i<avg.size(); i++){
            array[i] = avg.get(i).sock;
        }
        return array;
    }

    private void initSockAvg(){
        socks_avg.clear();
        for (int i=0; i<socks.length; i++){
            socks_avg.add(new SockAvg(socks[i], socks));
        }

        Collections.sort(socks_avg, new Comparator<SockAvg>(){
            public int compare(SockAvg one, SockAvg two){
                return (one.avg_distance < two.avg_distance) ? -1 : 1;
            }
        });
    }

    public double calcAvgDistance(Sock offer, Sock receive, Sock[] currentSocks){
        double avg_d = 0;
        for (int i=0; i<currentSocks.length; i++){
            if (currentSocks[i] == offer)
                continue;
            avg_d += receive.distance(currentSocks[i]);
        }
        avg_d = avg_d / (currentSocks.length-1);

        return avg_d;
    }

    public ArrayList<Sock> sortSocks(Sock[] unsorted){  
        //socks_sorted.clear();
        ArrayList<Sock> unsorted_list = new ArrayList<Sock>(Arrays.asList(this.socks));
        Collections.reverse(unsorted_list);
        ArrayList<Sock> sorted_list = new ArrayList<>();
        while (unsorted_list.size() >= 2){
            Sock first = unsorted_list.get(0);
            Sock second = unsorted_list.get(1);

            for (int i=1; i<unsorted_list.size(); i++){
                if (first.distance(unsorted_list.get(i)) < first.distance(second))
                    second = unsorted_list.get(i);
            }
            
            sorted_list.add(first); sorted_list.add(second);
            unsorted_list.remove(first); unsorted_list.remove(second);
        }
        
        return sorted_list;
    }

    public ArrayList<Sock> sortGreedy(ArrayList<Sock> unsorted){
        Sock c = calcCenter(unsorted);
        ArrayList<Sock> unsorted_temp = new ArrayList<Sock>(unsorted);
        Collections.sort(unsorted_temp, new Comparator<Sock>(){
            public int compare(Sock one, Sock two){
                if (one.distance(c) == two.distance(c))
                    return 0;
                else
                    return one.distance(c) < two.distance(c) ? 1 : -1;
            }
        });   
        ArrayList<Sock> sorted = new ArrayList<>();
        while (unsorted_temp.size() >= 2){
            Sock first = unsorted_temp.get(0);
            Sock second = unsorted_temp.get(1);

            for (int i=1; i<unsorted_temp.size(); i++){
                if (first.distance(unsorted_temp.get(i)) < first.distance(second))
                    second = unsorted_temp.get(i);
            }

            sorted.add(first); sorted.add(second);
            unsorted_temp.remove(first); unsorted_temp.remove(second);
        }

        return sorted;
    }

    public ArrayList<Sock> resortSocks(ArrayList<Sock> list){
        this.center = calcCenter(list);
        ArrayList<Sock> temp = list;
        Collections.sort(temp, new Comparator<Sock>(){
            public int compare(Sock one, Sock two){
                if (one.distance(center) == two.distance(center))
                    return 0;
                else
                    return one.distance(center) < two.distance(center) ? -1 : 1;
            }
        });   
        return temp;
    }

    public Pair findBestPair(ArrayList<Sock> list){
            Sock one = list.get(0);
            Sock two = list.get(1);
            for (int i=0; i<list.size(); i++){
                Sock outer = list.get(i);
                
                for (int j=0; j<list.size(); j++){
                    if (j==i)
                        continue;
                    Sock inner = list.get(j);
                    if (outer.distance(inner) < one.distance(two)){
                        one = outer;
                        two = inner;
                    }
                }
            }

        return new Pair(one, two);
    }

    private void removeFromPool(int numPairs){
        for (int i=0; i<numPairs; i++){
            Pair temp = findBestPair(sorted_list);
            paired.add(temp);
            sorted_list.remove(temp.left);
            sorted_list.remove(temp.right);
            unpairedIndex += 2;
        }
    }

    private void buildSockArray(ArrayList<Pair> pairedIn, ArrayList<Sock> unpairedIn){
        int counter=0;
        ArrayList<Sock> sortedIn = sortGreedy(unpairedIn);
        for (int i=0; i<pairedIn.size(); i++){
            socks[counter++] = pairedIn.get(i).left;
            socks[counter++] = pairedIn.get(i).right;
        }

        for (int i=0; i<sortedIn.size(); i++){
            socks[counter++] = sortedIn.get(i);
        }
        
    }

    private ArrayList<Sock> convertPairs(ArrayList<Pair> pairedIn){
        ArrayList<Sock> temp = new ArrayList<>();
        for (int i=0; i<pairedIn.size(); i++){
            temp.add(pairedIn.get(i).left);
            temp.add(pairedIn.get(i).right);
        }
        return temp;
    }

    private void buildUnpaired(ArrayList<Sock> unpairedIn){
        int counter = unpairedIndex;
        for (int i=0; i<unpairedIn.size(); i++){
            socks[counter++] = unpairedIn.get(i);
        }
    }

    private Sock calcCenter(ArrayList<Sock> list){
        double r, g, b; r = g = b = 0;
        int size = list.size();
        for (int i=0; i<size; i++){
            r += list.get(i).R;
            g += list.get(i).G;
            b += list.get(i).B;
        }
        r = Math.round(r/(double)size);
        g = Math.round(g/(double)size);
        b = Math.round(b/(double)size);

        return new Sock((int)r, (int)g, (int)b);
    }

    public void blossomPair(ArrayList<Sock> list) {
        Sock[] socks = list.toArray(new Sock[list.size()]);
        float[][] allPairs = new float[2*n*(2*n-1)/2][3];
            
        int k = 0;
        for(int i = 0; i < socks.length; i++){
            for(int j = i+1; j < socks.length; j++){
                allPairs[k] = new float[]{i,j, -(float)(socks[i].distance(socks[j]))};
                k++;
            }
        }
        int[] result = new Blossom(allPairs, true).maxWeightMatching();

        list.clear();

        for (int i = 0; i < result.length; i++) {
            if (result[i] < i) continue;
            list.add(socks[i]);
            list.add(socks[result[i]]);
        }
    }

    private void pairUp(){
        ArrayList<Sock> temp = new ArrayList<>();
        for (int i=0; i<paired.size(); i++){
            temp.add(paired.get(i).left);
            temp.add(paired.get(i).right);
        }
        temp.addAll(sorted_list);
        blossomPair(temp);

        for (int i=0; i<temp.size(); i+=2){
            Pair p = new Pair(temp.get(i), temp.get(i+1));
            pairedAll.add(p);
        }
        pairedAll = sortPairs(pairedAll);
     
    }
}