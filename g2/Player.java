package exchange.g2;

import java.util.*;

import exchange.sim.Offer;
import exchange.sim.Request;
import exchange.sim.Sock;
import exchange.sim.Transaction;

public class Player extends exchange.sim.Player {

    /*
        Inherited from exchange.sim.Player:
        Random random   -       Random number generator, if you need it

        Remark: you have to manually adjust the order of socks, to minimize the total embarrassment
                the score is calculated based on your returned list of getSocks(). Simulator will pair up socks 0-1, 2-3, 4-5, etc.
     */
    private HashMap<Integer, HashMap<Integer,Double>> THRESHOLD;
    private int threshold;
    private int offeringS1, offeringS2, id;
    private double offeringS1MarketValue, offeringS2MarketValue;
    private double[] offeringS1MarketValuePerPlayer, offeringS2MarketValuePerPlayer;

    private Sock[] socks;

    private Deque<RequestedSock> requestedSocks;
    private int requestsReceivedCounter; // Count the number of times our offered socks have been requested to benchmark.

    private int currentTurn;
    private int totalTurns;
    private int numPlayers;
    private int numSocks;
    private boolean shouldRecomputePairing;
    private PriorityQueue<Sock> lastGreedyPairing;

    private PriorityQueue<SockPair> rankedPairs;
    private HashMap<Sock, Integer> offerSocks;
    private List<Offer> lastOffers = null;
    private Map<Sock,Map<Sock,Double>> singleExchangeEmbarrasments;
    private Sock[] notConsidered;

    private int stagnationTracker;
    private boolean stagnant;

    private boolean largeNumSocks;
    private int CONSIDERED;

    @Override
    public void init(int id, int n, int p, int t, List<Sock> socks) {
        this.id = id;
        this.totalTurns = t;
        this.currentTurn = 0;
        this.numPlayers = p;
        this.numSocks = n*2;
        this.socks = (Sock[]) socks.toArray(new Sock[2 * n]);
        this.requestedSocks = new ArrayDeque<RequestedSock>();
        this.requestsReceivedCounter = 0;

        this.rankedPairs = new PriorityQueue<SockPair>();
        this.offerSocks = new HashMap<Sock, Integer>();
        this.THRESHOLD = createThreshold();
        this.threshold = 95;

        if(numSocks > 200) {
            largeNumSocks = true;
            if(totalTurns < 1000) {
                CONSIDERED = 200;
            } else {
                CONSIDERED = 100;
            }
        }

        pairAlgo();
        if(largeNumSocks) {
            notConsidered = Arrays.copyOfRange(this.socks,0,this.socks.length-CONSIDERED);
            this.socks = Arrays.copyOfRange(this.socks,this.socks.length-CONSIDERED,this.socks.length);
        }
        updateOfferSocks(this.threshold);
        this.shouldRecomputePairing = false;
        this.singleExchangeEmbarrasments = new HashMap<>();
    }

    private HashMap< Integer, HashMap<Integer,Double>> createThreshold(){
        HashMap<Integer, HashMap<Integer,Double>> result = new HashMap< Integer, HashMap<Integer,Double>>();

        HashMap<Integer,Double> n_10 = new HashMap<Integer,Double>();
        n_10.put(0,14.45683229);
        n_10.put(5,27.04070882);
        n_10.put(10,31.16629779);
        n_10.put(15,34.82567729);
        n_10.put(20,39.43039952);
        n_10.put(25,45.96228755);
        n_10.put(30,50.28188857);
        n_10.put(35,52.43379674);
        n_10.put(40,55.0781046);
        n_10.put(45,59.20605407);
        n_10.put(50,61.80862098);
        n_10.put(55,65.10040929);
        n_10.put(60,71.70616603);
        n_10.put(65,76.55716818);
        n_10.put(70,80.15536644);
        n_10.put(75,88.67706461);
        n_10.put(80,95.31330462);
        n_10.put(85,99.56778715);
        n_10.put(90,111.0624375);
        n_10.put(95,132.9023253);
        n_10.put(100,165.5173707);

        HashMap<Integer,Double> n_100 = new HashMap<Integer,Double>();
        n_100.put(0,1.414213562);
        n_100.put(5,11.35117446);
        n_100.put(10,14.45683229);
        n_100.put(15,16.99558441);
        n_100.put(20,18.83613249);
        n_100.put(25,20.53045432);
        n_100.put(30,22.12237714);
        n_100.put(35,24.0208243);
        n_100.put(40,25.63201124);
        n_100.put(45,27.12285462);
        n_100.put(50,28.97411483);
        n_100.put(55,30.39406074);
        n_100.put(60,32.54842459);
        n_100.put(65,34.27460854);
        n_100.put(70,36.28631663);
        n_100.put(75,38.73949498);
        n_100.put(80,41.01219331);
        n_100.put(85,44.17915406);
        n_100.put(90,47.75457836);
        n_100.put(95,53.57424692);
        n_100.put(100,96.64884893);

        result.put(10, n_10);
        result.put(100, n_100);

        return result;
    }
    
    private void updateOfferSocks(int threshold){
        HashMap<Sock, Integer> cloneOffer = new HashMap(this.offerSocks);
        this.offerSocks.clear();
        if(!largeNumSocks) {
        for(SockPair pair:rankedPairs){
            if (largeNumSocks==true || pair.distance > this.THRESHOLD.get(this.numSocks/2).get(threshold)){
                if(!cloneOffer.containsKey(pair.s1)){
                    this.offerSocks.put(pair.s1,0);
                } else {
                    this.offerSocks.put(pair.s1,cloneOffer.get(pair.s1));
                }
                if(!cloneOffer.containsKey(pair.s2)){
                    this.offerSocks.put(pair.s2,0);
                } else {
                    this.offerSocks.put(pair.s2,cloneOffer.get(pair.s2));
                }
            }
        }
        } else {
            int bound = 20;
            for(int i=socks.length-1;i>socks.length-1-bound;i--) {
                this.offerSocks.put(socks[i],0);
            }
        }
    }

    private double getEmbarrasment() {
        return getEmbarrasment(this.socks);
    }

    private double getEmbarrasment(Sock[] socks) {
        double result = 0;
        for (int i = 0; i < socks.length; i += 2){
            result += socks[i].distance(socks[i+1]);
        }
        return result;
    }

    public void pairBlossom() {
        this.socks = pairBlossom(this.socks, true);
    }

    public Sock[] pairBlossom(Sock[] socks) {
        return pairBlossom(socks, false);
    }

    public Sock[] pairAlgo(Sock[] socks) {
        Sock[] result;
        if(!largeNumSocks){
            result = pairBlossom(socks);
        } else {
            result  = pairGreedily(socks);
        }
        return result;
    }


    public void pairAlgo() {
        if(!largeNumSocks){
            this.socks = pairBlossom(this.socks, true);
        } else {
            this.socks = pairGreedily(this.socks, true);
        }
   }

   public void pairGreedily() {
        this.socks = pairGreedily(this.socks, true);
   }

   public Sock[] pairGreedily(Sock[] socks) {
        return pairGreedily(socks, false);
   }

   private Sock[] pairGreedily(Sock[] socks, boolean updateRankedPairs) {
       if(updateRankedPairs==false)
           socks = Arrays.copyOfRange(socks,socks.length-CONSIDERED,socks.length);
       PriorityQueue<SockPair> queue = new PriorityQueue<SockPair>(socks.length*socks.length, Collections.reverseOrder());
        for (int i = 0; i < socks.length ; i++){
            for (int j = 0; j < i; j++){
                queue.add(new SockPair(socks[i],socks[j]));
            }
        }
        
        HashSet<Sock> matched = new HashSet<Sock>();
        while( matched.size() < socks.length && queue.size()!=0){
            SockPair pair = queue.poll();
            if(pair != null) {
                if(!matched.contains(pair.s1) && !matched.contains(pair.s2) && pair.s1!=pair.s2){
                    matched.add(pair.s1);
                    socks[matched.size()-1] = pair.s1;
                    matched.add(pair.s2);
                    socks[matched.size()-1] = pair.s2;
                    if (updateRankedPairs) {
                        this.rankedPairs.add(pair);
                    }
                }
            }
        }
        
        return socks;
   }

    public Sock[] pairBlossom(Sock[] socks, boolean updateRankedPairs) {
        int[] match = new Blossom(getCostMatrix(socks), true).maxWeightMatching();
        List<Sock> result = new ArrayList<Sock>();
        for (int i=0; i<match.length; i++) {
            if (match[i] < i) continue;
            result.add(socks[i]);
            result.add(socks[match[i]]);
            if (updateRankedPairs) {
                this.rankedPairs.add(new SockPair(socks[i], socks[match[i]]));
            }
        }
        return (Sock[]) result.toArray(new Sock[socks.length]);
    }

    private float[][] getCostMatrix(Sock[] socks) {
        int numSocks = socks.length;
        float[][] matrix = new float[numSocks*(numSocks-1)/2][3];
        int idx = 0;
        for (int i = 0; i < socks.length; i++) {
            for (int j=i+1; j< socks.length; j++) {
                matrix[idx] = new float[]{i, j, (float)(-socks[i].distance(socks[j]))};
                idx ++;
            }
        }
        return matrix;
    }

    private double getMaxReductionInPairDistance(Sock s) {
        double maxDistanceReduction = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < this.socks.length; i+=2) {
            if (i == offeringS1 || i == offeringS2) continue; // Skip offered pair.
            double pairDistance = this.socks[i].distance(this.socks[i+1]);
            double distanceToFirst = this.socks[i].distance(s);
            double distanceToSecond = this.socks[i+1].distance(s);
            double distanceReduction = pairDistance - Math.min(distanceToFirst, distanceToSecond);
            if (distanceReduction > maxDistanceReduction) {
                maxDistanceReduction = distanceReduction;
            }
        }
        return maxDistanceReduction;
    }

    @Override
    public Offer makeOffer(List<Request> lastRequests, List<Transaction> lastTransactions) {
        currentTurn++;
        if (this.shouldRecomputePairing) {
            rankedPairs.clear();
            if(stagnant==true) {
                Sock[] temp = new Sock[numSocks];
                System.arraycopy(notConsidered,0,temp,0,notConsidered.length);
                System.arraycopy(socks,0,temp,notConsidered.length,socks.length);
                this.socks = temp;
                pairAlgo();
                
                notConsidered = Arrays.copyOfRange(this.socks,0,this.socks.length-CONSIDERED);
                this.socks = Arrays.copyOfRange(this.socks,this.socks.length-CONSIDERED,this.socks.length);
                stagnant = false;
            } else {
                pairAlgo();
            }
            this.shouldRecomputePairing = false;
            singleExchangeEmbarrasments.clear();
            updateOfferSocks(this.threshold);
        }

        boolean twice = true;
        for(int times : this.offerSocks.values()) {
            if(times<2) {
                twice = false;
                break;
            }
        }

        while (twice == true || this.offerSocks.size() == 0) {
            this.threshold -= 5;
            updateOfferSocks(this.threshold);

            double sum = 0.0;
            for (double d : this.offerSocks.values()) {
                sum += d;
            }
            twice = false;
        }
        
        //------------------------------------------------------------------------------------------------------------
        // Market value
        //------------------------------------------------------------------------------------------------------------

        // Get all transacted socks, since we will ignore them when recording the requests.
        List<Sock> transactedSocks = new ArrayList<Sock>();
        for (Transaction t : lastTransactions) {
            transactedSocks.add(t.getFirstSock());
            transactedSocks.add(t.getSecondSock());
        }

        // Update the requested socks so far.
        int rank;
        for (int i = 0; i < lastRequests.size(); ++i) {
            Request request = lastRequests.get(i);
            if (i == id || request == null) continue; // Skip if it's our own request or null.

            if(request.getFirstID() >= 0 && request.getFirstRank() >= 0) {
                if (request.getFirstID() == id) requestsReceivedCounter++;
                rank = request.getFirstRank();
                Sock first = lastOffers.get(request.getFirstID()).getSock(rank);
                if (!transactedSocks.contains(first)) {
                    RequestedSock firstRequested = new RequestedSock(first, i, rank, currentTurn);
                    requestedSocks.addFirst(firstRequested);
                }
            }

            if(request.getSecondID() >= 0 && request.getSecondRank() >= 0) {
                if (request.getSecondID() == id) requestsReceivedCounter++;
                rank = request.getSecondRank();
                Sock second = lastOffers.get(request.getSecondID()).getSock(request.getSecondRank());
                if (!transactedSocks.contains(second)) {
                    RequestedSock secondRequested = new RequestedSock(second, i, rank, currentTurn);
                    requestedSocks.addFirst(secondRequested);
                }
            }
        }

        Sock maxMarketValueSock = null;  // Default for the first turn, when there are no requested socks yet.
        double maxMarketValue = -1;
        double[] maxMarketValuePerPlayer = new double[numPlayers];

        Sock secondMaxMarketValueSock = null;
        double secondMaxMarketValue = -1;
        double[] secondMarketValuePerPlayer = new double[numPlayers];
        
        for(Sock next : this.offerSocks.keySet()) {
            double marketValue = 0.0;
            double[] marketValueByPlayer = new double[numPlayers];
            for (RequestedSock rs : requestedSocks) {
                // Right now we are allowing a sock to be compared with itself, which will give it a high market value
                // if it has been requested (since the distance will be minimal). To disallow this check that !rs.equals(next.s1).
                if (!rs.equals(next)) {
                    marketValue += rs.getPartialMarketValue(next, currentTurn);
                    marketValueByPlayer[rs.playerId] += rs.getPartialMarketValue(next, currentTurn);
                }
            }

            if(this.offerSocks.get(next) != null) marketValue /= (this.offerSocks.get(next) + 1);

            if (marketValue > maxMarketValue) {
                secondMaxMarketValue = maxMarketValue;
                secondMaxMarketValueSock = maxMarketValueSock;
                maxMarketValue = marketValue;
                maxMarketValueSock = next;
                maxMarketValuePerPlayer = marketValueByPlayer;
            } else if (marketValue > secondMaxMarketValue) {
                secondMaxMarketValue = marketValue;
                secondMaxMarketValueSock = next;
                secondMarketValuePerPlayer = marketValueByPlayer;
            }
        }
        
        // We need to find them individually since they might be part of the same pair.
        List<Sock> temp = Arrays.asList(socks);
        this.offerSocks.put(maxMarketValueSock, this.offerSocks.get(maxMarketValueSock) + 1);
        this.offerSocks.put(secondMaxMarketValueSock, this.offerSocks.get(secondMaxMarketValueSock) + 1);
        
        offeringS1 = temp.indexOf(maxMarketValueSock);
        offeringS2 = temp.indexOf(secondMaxMarketValueSock);

        offeringS1MarketValue = maxMarketValue;
        offeringS2MarketValue = secondMaxMarketValue;

        offeringS1MarketValuePerPlayer = maxMarketValuePerPlayer;
        offeringS2MarketValuePerPlayer = secondMarketValuePerPlayer;

        return new Offer(maxMarketValueSock, secondMaxMarketValueSock);
    }

    @Override
    public Request requestExchange(List<Offer> offers) {
        try {
            lastOffers = offers;

            int firstId = -1;
            int firstRank = -1;
            int secondId = -1;
            int secondRank = -1;

            Sock[] socksNoId1 = this.socks.clone();
            Sock[] socksNoId2 = this.socks.clone();
            Sock[] socksNoId1NorId2 = this.socks.clone();

            double currentEmbarrasment = getEmbarrasment();
            double avgEmbarrasment;
            double embarrasmentExchangingId1ForS1 = 0;
            double embarrasmentExchangingId2ForS1 = 0;
            double embarrasmentExchangingId1ForS2 = 0;
            double embarrasmentExchangingId2ForS2 = 0;
            double embarrasmentExchangingId1AndId2 = 0;
            double minPairEmbarrasment = currentEmbarrasment;

            double minSingleEmbarrasment = currentEmbarrasment;
            int singleId = -1;
            int singleRank = -1;

            Sock sock1 = socks[offeringS1];
            Sock sock2 = socks[offeringS2];
            
            boolean keepLooking = true;
            for (int i = 0; i < offers.size() && keepLooking; ++i) {
                if (i == id) continue; // Skip our own offer.
                for (int j = 1; j < 3 && keepLooking; ++j) {
                    Sock s1 = offers.get(i).getSock(j);
                    if (s1 == null) continue;

                    socksNoId1[offeringS1] = s1;
                    if (!singleExchangeEmbarrasments.containsKey(s1)) singleExchangeEmbarrasments.put(s1,new HashMap<>());
                    if (!singleExchangeEmbarrasments.get(s1).containsKey(sock1)) { 
                        singleExchangeEmbarrasments.get(s1).put(sock1,getEmbarrasment(pairAlgo(socksNoId1)));
                    }
                    embarrasmentExchangingId1ForS1 = singleExchangeEmbarrasments.get(s1).get(sock1);
                    if (embarrasmentExchangingId1ForS1 > currentEmbarrasment) continue;

                    socksNoId2[offeringS2] = s1;
                    if (!singleExchangeEmbarrasments.get(s1).containsKey(sock2)) { 
                        singleExchangeEmbarrasments.get(s1).put(sock2,getEmbarrasment(pairAlgo(socksNoId2)));
                    }
                    embarrasmentExchangingId2ForS1 = singleExchangeEmbarrasments.get(s1).get(sock2);
                    if (embarrasmentExchangingId2ForS1 > currentEmbarrasment) continue;

                    // Rank 1 sock has a higher chance to be traded, so the average has to be weighted
                    avgEmbarrasment = (2*embarrasmentExchangingId1ForS1 + embarrasmentExchangingId2ForS1)/3;
                    if (avgEmbarrasment < minSingleEmbarrasment) {
                        minSingleEmbarrasment = avgEmbarrasment;
                        if(largeNumSocks) {
                            minPairEmbarrasment = avgEmbarrasment;
                            secondRank = firstRank;
                            secondId = firstId;
                            firstId = i;
                            firstRank = j;
                        }

                        singleId = i;
                        singleRank = j;
                        keepLooking = true;
                    }

                    if(!largeNumSocks) {
                    
                    socksNoId1NorId2[offeringS1] = s1;
                    for (int k = i; k < offers.size() && keepLooking; ++k) {
                        if (keepLooking == false) break;
                        if (k == id) continue; // Skip our own offer.
                        for (int l = j+1; l < 3 && keepLooking; ++l) {
                            Sock s2 = offers.get(k).getSock(l);
                            if (s2 == null) continue;

                            socksNoId1[offeringS1] = s2;
                            if (!singleExchangeEmbarrasments.containsKey(s2)) singleExchangeEmbarrasments.put(s2,new HashMap<>());
                            if (!singleExchangeEmbarrasments.get(s2).containsKey(sock1)) { 
                                singleExchangeEmbarrasments.get(s2).put(sock1,getEmbarrasment(pairAlgo(socksNoId1)));
                            }
                            embarrasmentExchangingId1ForS2 = singleExchangeEmbarrasments.get(s2).get(sock1);
                            if (embarrasmentExchangingId1ForS2 > currentEmbarrasment) continue;

                            socksNoId2[offeringS2] = s2;
                            if (!singleExchangeEmbarrasments.get(s2).containsKey(sock2)) { 
                                singleExchangeEmbarrasments.get(s2).put(sock2,getEmbarrasment(pairAlgo(socksNoId2)));
                            }
                            embarrasmentExchangingId2ForS2 = singleExchangeEmbarrasments.get(s2).get(sock2);
                            if (embarrasmentExchangingId2ForS2 > currentEmbarrasment) continue;

                            socksNoId1NorId2[offeringS2] = s2;
                            if(totalTurns < 100)
                               embarrasmentExchangingId1AndId2 = getEmbarrasment(pairAlgo(socksNoId1NorId2));

                            if (embarrasmentExchangingId1AndId2 > currentEmbarrasment) continue;
                            avgEmbarrasment = (2*embarrasmentExchangingId1ForS1 + 2*embarrasmentExchangingId1ForS2 +
                                    embarrasmentExchangingId2ForS1 + embarrasmentExchangingId2ForS2 +
                                    embarrasmentExchangingId1AndId2) / 7;
                            if (avgEmbarrasment < minPairEmbarrasment) {
                                minPairEmbarrasment = avgEmbarrasment;
                                firstId = i;
                                firstRank = j;
                                secondId = k;
                                secondRank = l;
                                if(totalTurns > 100 && avgEmbarrasment < currentEmbarrasment) {
                                    keepLooking = false;
                                    break;
                                }
                            }
                        }
                    }
                    }
                }
            }

            if(firstId == -1 && singleId == -1 && largeNumSocks==true) {
                if(++stagnationTracker >= 5) {
                    stagnationTracker = 0;
                    stagnant = true;
                }
            }

            if (minSingleEmbarrasment < minPairEmbarrasment && !largeNumSocks) {
                return new Request(singleId, singleRank, -1, -1);
            } else {
                return new Request(firstId, firstRank, secondId, secondRank);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Request(-1, -1, -1, -1);
        }
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
        this.shouldRecomputePairing = true;

        int rank;
        Sock newSock;
        if (transaction.getFirstID() == id) {
            rank = transaction.getFirstRank();
            newSock = transaction.getSecondSock();
        } else {
            rank = transaction.getSecondRank();
            newSock = transaction.getFirstSock();
        }
        if (rank == 1) socks[offeringS1] = newSock;
        else socks[offeringS2] = newSock;
    }

    @Override
    public List<Sock> getSocks() {
        pairAlgo();
        if(currentTurn == totalTurns && largeNumSocks) {
            Sock[] temp = new Sock[numSocks];
            System.arraycopy(notConsidered,0,temp,0,notConsidered.length);
            System.arraycopy(socks,0,temp,notConsidered.length,socks.length);
            socks = temp;

            socks = pairBlossom(this.socks, true);
        }
        return Arrays.asList(socks);
    }
}
