package games.dominion;

import core.*;
import core.actions.*;
import games.dominion.actions.*;
import games.dominion.cards.*;
import games.dominion.DominionConstants.*;
import utilities.Utils;

import java.util.*;

import static java.util.stream.Collectors.*;

public class DominionForwardModel extends AbstractForwardModel {
    /**
     * Performs initial game setup according to game rules
     * - sets up decks and shuffles
     * - gives player cards
     * - places tokens on boards
     * etc.
     *
     * @param firstState - the state to be modified to the initial game state.
     */
    @Override
    protected void _setup(AbstractGameState firstState) {
        firstState.setGamePhase(DominionGameState.DominionGamePhase.Play);
        // Nothing to do yet - this is all done by firstState._reset() which is always called immediately before this
    }

    /**
     * Applies the given action to the game state and executes any other game rules. Steps to follow:
     * - execute player action
     * - execute any game rules applicable
     * - check game over conditions, and if any trigger, set the gameStatus and playerResults variables
     * appropriately (and return)
     * - move to the next player where applicable
     *
     * @param currentState - current game state, to be modified by the action.
     * @param action       - action requested to be played by a player.
     */
    @Override
    protected void _next(AbstractGameState currentState, AbstractAction action) {
        DominionGameState state = (DominionGameState) currentState;

        if (!state.actionsInProgress.isEmpty()) {
            // we just register the action with the currently active action
            state.actionsInProgress.peek().registerActionTaken(state, action);
        }

        action.execute(state);

        // we may be in an extended action, so update that
        if (!state.actionsInProgress.isEmpty()) {
            // we just register the action taken with the currently active action
            // and then remove anything which is now complete
            int loopCount = 0;
            while (!state.actionsInProgress.isEmpty() && state.actionsInProgress.peek().executionComplete(state)) {
                state.actionsInProgress.pop();
                loopCount++;
                if (loopCount > 100) {
                    throw new AssertionError("WTF?");
                }
            }
        }
        int playerID = state.getCurrentPlayer();

        switch (state.getGamePhase().toString()) {
            case "Play":
                if (!state.isActionInProgress() &&
                        (state.actionsLeftForCurrentPlayer < 1 || action instanceof EndPhase)) {
                    // change phase
                    // no change to current player
                    state.setGamePhase(DominionGameState.DominionGamePhase.Buy);
                    processDelayedActions(TriggerType.StartBuy, state);
                    // it would be possible to do this within setGamePhase, but we choose to keep this triggering code
                    // in the forward model for the moment.
                }
                break;
            case "Buy":
                if (state.buysLeftForCurrentPlayer < 1 || action instanceof EndPhase) {
                    // change phase
                    if (state.gameOver()) {
                        endOfGameProcessing(state);
                    } else {
                        state.endOfTurn(playerID);
                    }
                }
                break;
            default:
                throw new AssertionError("Unknown Game Phase " + state.getGamePhase());
        }

    }

    private void endOfGameProcessing(DominionGameState state) {
        state.setGameStatus(Utils.GameResult.GAME_END);
        int[] finalScores = new int[state.playerCount];
        for (int p = 0; p < state.playerCount; p++) {
            int finalP = p;
            finalScores[p] = state.getTotal(p, c -> c.victoryPoints(finalP, state));
        }
        int winningScore = Arrays.stream(finalScores).max().getAsInt();
        for (int p = 0; p < state.playerCount; p++) {
            state.setPlayerResult(finalScores[p] == winningScore ? Utils.GameResult.WIN : Utils.GameResult.LOSE, p);
        }
    }

    private void processDelayedActions(TriggerType trigger, DominionGameState state) {
        Map<Boolean, List<IDelayedAction>> partition = state.delayedActions.stream()
                .collect(partitioningBy(a -> a.getTrigger() == trigger));

        state.delayedActions = partition.get(false); // the ones we are not executing...put them back
        partition.get(true).forEach(a -> a.execute(state));
    }

    /**
     * Calculates the list of currently available actions, possibly depending on the game phase.
     *
     * @param gameState
     * @return - List of IAction objects.
     */
    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        DominionGameState state = (DominionGameState) gameState;
        int playerID = state.getCurrentPlayer();

        switch (state.getGamePhase().toString()) {
            case "Play":
                if (state.isActionInProgress()) {
                    return state.actionsInProgress.peek().followOnActions(state);
                }
                if (state.actionsLeft() > 0) {
                    Set<DominionCard> actionCards = state.getDeck(DeckType.HAND, playerID).stream()
                            .filter(DominionCard::isActionCard).collect(toSet());
                    List<AbstractAction> availableActions = actionCards.stream().map(dc -> dc.getAction(playerID))
                            .distinct().collect(toList());
                    availableActions.add(new EndPhase());
                    return availableActions;
                }
                return Arrays.asList(new EndPhase());
            case "Buy":
                // we return every available card for purchase within our price range
                int budget = state.availableSpend(playerID);
                List<AbstractAction> options = state.cardsToBuy().stream()
                        .filter(ct -> ct.cost <= budget)
                        .sorted(Comparator.comparingInt(c -> -c.cost))
                        .map(ct -> new BuyCard(ct, playerID))
                        .collect(toList());
                options.add(new EndPhase());
                return options;
            default:
                throw new AssertionError("Unknown Game Phase " + state.getGamePhase());
        }
    }

    /**
     * Gets a copy of the FM with a new random number generator.
     *
     * @return - new forward model with different random seed (keeping logic).
     */
    @Override
    protected AbstractForwardModel _copy() {
        // no internal state as yet
        return this;
    }
}
