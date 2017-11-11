package exchange.g2;

import exchange.sim.Sock;

public class RequestedSock extends Sock {

    public int playerId;
    public int rank;
    public int turn;


    public RequestedSock(Sock s, int playerId, int rank, int turn) {
        super(s);
        this.playerId = playerId;
        this.rank = rank;
        this.turn = turn;
    }

    public double getPartialMarketValue(Sock s, int currentTurn) {
        /*
         This is the market value that the given sock would have when compared with this requested sock.
         The market value is:
          - Inversely proportional to the rank of the request, so that rank 2 will return half market value compared
            with rank 1.
          - Inversely proportional to the distance between the socks, i.e. ehe closer they are the higher the market value.
          - Inversely proportional to the number of turns that passed since the sock was requested, so that a more recent
            request will provide a higher market value.
         */
        return 1.0 / (
                this.rank *
                (this.distance(s) + 1) * // Add 1 so that a sock compared to itself doesn't cause a division by 0.
                (currentTurn - this.turn + 1) // Add 1 so that requests in this turn don't cause a division by 0.
        );
    }
}