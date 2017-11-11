package exchange.g3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import exchange.sim.*;

public class Player extends exchange.sim.Player {
    /*
        Inherited from exchange.sim.Player:
        Remark: you have to manually adjust the order of socks, to minimize the total embarrassment
                the score is calculated based on your returned list of getSocks(). Simulator will pair up socks 0-1, 2-3, 4-5, etc.
     */
    private int id;
    private Sock s1, s2;
    private SockCollection socksCollection;
    private int turn;
    private int timeSinceTransaction = 0;
    private boolean transactionOccurred = false;

    private int timeSinceRequest;

    private int totalTurns;
    private List<Offer> offs;

    @Override
    public void init(int id, int n, int p, int t, List<Sock> socks) {
        this.id = id;
        this.turn = t;
        this.totalTurns = t;
        this.socksCollection = new SockCollection(socks, id);

        this.offs = new ArrayList<Offer>();
    }

    @Override
    public Offer makeOffer(List<Request> lastRequests, List<Transaction> lastTransactions) {
        /*
			lastRequests.get(i)		-		Player i's request last round
			lastTransactions		-		All completed transactions last round.
		 */
        ArrayList<Sock> recentlyDesiredSocks = new ArrayList<Sock>();
        // If there were offers in the previous round and we are half-way there.
        if (offs.size() > 0 && turn <= totalTurns/2) {
            HashMap<Integer, ArrayList<Sock>> playersRecentlyRequestedSocks = new HashMap<Integer, ArrayList<Sock>>();
            HashMap<Integer, ArrayList<Sock>> playersRecentlyReceivedSocks = new HashMap<Integer, ArrayList<Sock>>();

            List<Offer> recentOffers = offs;
            List<Request> recentRequests = lastRequests;
            List<Transaction> recentTransactions = lastTransactions;
            Request requestOfI;
            Sock firstSockRequestedByI = null;
            Sock secondSockRequestedByI = null;

            for (int i = 0; i < lastRequests.size(); i++) {
                if (i != id) {
                    playersRecentlyRequestedSocks.put(i, new ArrayList<Sock>());
                    playersRecentlyReceivedSocks.put(i, new ArrayList<Sock>());
                    requestOfI = recentRequests.get(i);
                    if (!((requestOfI == null) || requestOfI.getFirstID() == -1)) {
                        firstSockRequestedByI = recentOffers.get(requestOfI.getFirstID()).getSock(requestOfI.getFirstRank());
                        playersRecentlyRequestedSocks.get(i).add(firstSockRequestedByI);
                    }
                    if (!((requestOfI == null) || requestOfI.getSecondID() == -1)) {
                        secondSockRequestedByI = recentOffers.get(requestOfI.getSecondID()).getSock(requestOfI.getSecondRank());
                        playersRecentlyRequestedSocks.get(i).add(secondSockRequestedByI);
                    }
                }
            }

            for (Transaction tr : recentTransactions) {
                if (tr.getFirstID() != id) {
                    playersRecentlyReceivedSocks.get(tr.getFirstID()).add(tr.getSecondSock());
                }
                if (tr.getSecondID() != id) {
                    playersRecentlyReceivedSocks.get(tr.getSecondID()).add(tr.getFirstSock());
                }
            }
            for (int i = 0; i < lastRequests.size(); i++) {
                if (i != id) {
                    for (Sock s : playersRecentlyRequestedSocks.get(i)) {
                        if (!playersRecentlyReceivedSocks.get(i).contains(s)) {
                            recentlyDesiredSocks.add(s);
                        }
                    }
                }
            }
        }

        // Tracks number of turns left.
        this.turn -= 1;

        if (!transactionOccurred) {
            ++timeSinceTransaction;
        }

        transactionOccurred = false;

        if (timeSinceTransaction > 2) {
            // Re-order collection if it's been a while since a transaction has occurred.
            this.socksCollection.shuffle(true);
            timeSinceTransaction = 0;
        }

        if (timeSinceRequest > 5) {
            // Change the threshold and reorder collection if it's been
            // a while since we requested for socks.
            this.socksCollection.shuffle(false);
            timeSinceRequest = 0;
        }

        Sock[] worstPairSocks = this.socksCollection.getWorstPairSocks(recentlyDesiredSocks);
        this.s1 = worstPairSocks[0];
        this.s2 = worstPairSocks[1];
        return new Offer(s1, s2);
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
        this.offs = offers;

        Request currentOffer = socksCollection.requestBestOffer(offers);
        if (currentOffer.getFirstID() == -1 && currentOffer.getSecondID() ==-1) {
            timeSinceRequest++;
        } else {
            timeSinceRequest = 0;
        }

        return currentOffer;
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

        //remove the sock we are getting rid of aka s1
        //find its pair and move it to good and shift pivot
        if (rank == 1) {
            socksCollection.putSock(s1, newSock);
        }
        else {
            socksCollection.putSock(s2, newSock);
        }

        transactionOccurred = true;
        timeSinceTransaction = 0;
    }

    @Override
    public List<Sock> getSocks() {
        boolean rePair = false;
        if (turn < 1) {
            rePair = true;
        }

        return socksCollection.getCollection(rePair);
    }
}
