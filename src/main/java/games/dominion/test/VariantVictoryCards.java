package games.dominion.test;

import core.AbstractPlayer;
import games.dominion.*;

import games.dominion.cards.CardType;
import games.dominion.cards.DominionCard;
import org.junit.*;

import java.util.*;

import static games.dominion.DominionConstants.*;
import static java.util.stream.Collectors.*;
import static org.junit.Assert.*;

public class VariantVictoryCards {


    Random rnd = new Random(373);
    List<AbstractPlayer> players = Arrays.asList(new TestPlayer(),
            new TestPlayer(),
            new TestPlayer(),
            new TestPlayer());

    DominionGame game = new DominionGame(players, DominionParameters.firstGame(System.currentTimeMillis()));
    DominionGame gameImprovements = new DominionGame(players, DominionParameters.improvements(System.currentTimeMillis()));
    DominionForwardModel fm = new DominionForwardModel();

    @Test
    public void oneGarden() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.GARDENS, 1, DeckType.DISCARD);
        state.getDeck(DeckType.HAND, 1).remove(DominionCard.create(CardType.COPPER));
        int startScore = 3;
        for (int addedCards = 0; addedCards < 33; addedCards++) {
            for (int p = 0; p < 4; p++) {
                assertEquals(startScore + (p == 1 ? 1 + (addedCards / 10) : 0), (int) state.getGameScore(p));
            }
            state.addCard(CardType.COPPER, 1, DeckType.DISCARD);
        }
    }

    @Test
    public void twoGardens() {}

}
