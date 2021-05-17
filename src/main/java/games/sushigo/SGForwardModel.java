package games.sushigo;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.CoreConstants;
import core.actions.AbstractAction;
import core.components.Deck;
import games.sushigo.actions.ChopSticksAction;
import games.sushigo.actions.DebugAction;
import games.sushigo.actions.NigiriWasabiAction;
import games.sushigo.actions.PlayCardAction;
import games.sushigo.cards.SGCard;
import utilities.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.util.stream.Collectors.joining;

public class SGForwardModel extends AbstractForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        SGGameState SGGS = (SGGameState) firstState;
        SGParameters parameters = (SGParameters) SGGS.getGameParameters();

        //Setup player scores
        SGGS.playerScore = new int[firstState.getNPlayers()];
        SGGS.playerCardPicks = new int[firstState.getNPlayers()];
        SGGS.playerExtraCardPicks = new int[firstState.getNPlayers()];
        SGGS.playerTempuraAmount = new int[firstState.getNPlayers()];
        SGGS.playerSashimiAmount = new int[firstState.getNPlayers()];
        SGGS.playerDumplingAmount = new int[firstState.getNPlayers()];
        SGGS.playerWasabiAvailable = new int[firstState.getNPlayers()];
        SGGS.playerChopSticksAmount = new int[firstState.getNPlayers()];
        SGGS.playerScoreToAdd = new int[firstState.getNPlayers()];
        SGGS.playerChopsticksActivated = new boolean[firstState.getNPlayers()];
        SGGS.playerExtraTurns = new int[firstState.getNPlayers()];
        for (int i = 0; i < SGGS.getPlayerCardPicks().length; i++)
        {
            SGGS.getPlayerCardPicks()[i] = -1;
        }


        //Setup draw & discard piles
        SetupDrawpile(SGGS);
        SGGS.discardPile = new Deck<SGCard>("Discard pile", CoreConstants.VisibilityMode.VISIBLE_TO_ALL);

        //Setup player hands and fields
        SGGS.playerHands = new ArrayList<>();
        SGGS.playerFields = new ArrayList<>();
        switch (firstState.getNPlayers())
        {
            case 2:
                SGGS.cardAmount = parameters.cardAmountTwoPlayers;
                break;
            case 3:
                SGGS.cardAmount = parameters.cardAmountThreePlayers;
                break;
            case 4:
                SGGS.cardAmount = parameters.cardAmountFourPlayers;
                break;
            case 5:
                SGGS.cardAmount = parameters.cardAmountFivePlayers;
                break;

        }
        for (int i = 0; i < SGGS.getNPlayers(); i++){
            SGGS.playerHands.add(new Deck<SGCard>("Player" + i + " hand", CoreConstants.VisibilityMode.VISIBLE_TO_OWNER));
            SGGS.playerFields.add(new Deck<SGCard>("Player" + "Card field", CoreConstants.VisibilityMode.VISIBLE_TO_ALL));
        }
        DrawNewHands(SGGS);

        SGGS.getTurnOrder().setStartingPlayer(0);
    }

    public void DrawNewHands(SGGameState SGGS)
    {
        for (int i = 0; i < SGGS.getNPlayers(); i++){
            for (int j = 0; j < SGGS.cardAmount; j++)
            {
                SGGS.playerHands.get(i).add(SGGS.drawPile.draw());
            }
        }
    }

    private void SetupDrawpile(SGGameState SGGS)
    {
        SGParameters parameters = (SGParameters) SGGS.getGameParameters();
        SGGS.drawPile = new Deck<SGCard>("Draw pile", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        for (int i = 0; i < parameters.nMaki_3Cards; i++)
        {
            SGGS.drawPile.add(new SGCard(SGCard.SGCardType.Maki_3));
        }
        for (int i = 0; i < parameters.nMaki_2Cards; i++)
        {
            SGGS.drawPile.add(new SGCard(SGCard.SGCardType.Maki_2));
        }
        for (int i = 0; i < parameters.nMaki_1Cards; i++)
        {
            SGGS.drawPile.add(new SGCard(SGCard.SGCardType.Maki_1));
        }
        for (int i = 0; i < parameters.nChopstickCards; i++)
        {
            SGGS.drawPile.add(new SGCard(SGCard.SGCardType.Chopsticks));
        }
        for (int i = 0; i < parameters.nTempuraCards; i++)
        {
            SGGS.drawPile.add(new SGCard(SGCard.SGCardType.Tempura));
        }
        for (int i = 0; i < parameters.nSashimiCards; i++)
        {
            SGGS.drawPile.add(new SGCard(SGCard.SGCardType.Sashimi));
        }
        for (int i = 0; i < parameters.nDumplingCards; i++)
        {
            SGGS.drawPile.add(new SGCard(SGCard.SGCardType.Dumpling));
        }
        for (int i = 0; i < parameters.nSquidNigiriCards; i++)
        {
            SGGS.drawPile.add(new SGCard(SGCard.SGCardType.SquidNigiri));
        }
        for (int i = 0; i < parameters.nSalmonNigiriCards; i++)
        {
            SGGS.drawPile.add(new SGCard(SGCard.SGCardType.SalmonNigiri));
        }
        for (int i = 0; i < parameters.nEggNigiriCards; i++)
        {
            SGGS.drawPile.add(new SGCard(SGCard.SGCardType.EggNigiri));
        }
        for (int i = 0; i < parameters.nWasabiCards; i++)
        {
            SGGS.drawPile.add(new SGCard(SGCard.SGCardType.Wasabi));
        }
        for (int i = 0; i < parameters.nPuddingCards; i++)
        {
            SGGS.drawPile.add(new SGCard(SGCard.SGCardType.Pudding));
        }
        SGGS.drawPile.shuffle(new Random());
    }

    @Override
    protected void _next(AbstractGameState currentState, AbstractAction action) {
        if(currentState.getGameStatus() == Utils.GameResult.GAME_END) return;

        //Perform action
        action.execute(currentState);

        //Rotate deck and reveal cards
        SGGameState SGGS = (SGGameState)currentState;
        int turn = SGGS.getTurnOrder().getTurnCounter();
        if((turn + 1) % SGGS.getNPlayers() == 0 && SGGS.getPlayerExtraTurns(SGGS.getCurrentPlayer()) <= 0)
        {
            RevealCards(SGGS);
            RotateDecks(SGGS);
            RemoveUsedChopsticks(SGGS);

            //Clear points
            for(int i = 0; i < SGGS.getNPlayers(); i++)
            {
                SGGS.setPlayerScoreToAdd(i, 0);
            }
            //clear picks
            for (int i = 0; i < SGGS.getPlayerCardPicks().length; i++)
            {
                SGGS.getPlayerCardPicks()[i] = -1;
            }
        }


        //Check if game/round over
        if(IsRoundOver(SGGS) && SGGS.getPlayerExtraTurns(SGGS.getCurrentPlayer()) <= 0)
        {
            GiveMakiPoints(SGGS);
            if(SGGS.getTurnOrder().getRoundCounter() >= 2)
            {
                GivePuddingPoints(SGGS);
                SetWinner(SGGS);
                currentState.setGameStatus(Utils.GameResult.GAME_END);
                return;
            }
            SGGS.getTurnOrder().endRound(currentState);
            return;
        }

        //End turn
        if (currentState.getGameStatus() == Utils.GameResult.GAME_ONGOING) {
            if(SGGS.getPlayerChopSticksActivated(SGGS.getCurrentPlayer()) && SGGS.getPlayerExtraTurns(SGGS.getCurrentPlayer()) > 0)
            {
                SGGS.setPlayerExtraTurns(SGGS.getCurrentPlayer(), SGGS.getPlayerExtraTurns(SGGS.getCurrentPlayer()) - 1);
                return;
            }

            //reset chopstick activation and end turn
            SGGS.setPlayerChopsticksActivated(SGGS.getCurrentPlayer(), false);
            currentState.getTurnOrder().endPlayerTurn(currentState);
        }
    }

    private void RemoveUsedChopsticks(SGGameState SGGS) {
        for(int i = 0; i < SGGS.getNPlayers(); i++)
        {
            
        }
    }

    private void SetWinner(SGGameState SGGS) {
        int currentBestScore = 0;
        List<Integer> winners = new ArrayList<>();
        for(int i = 0; i < SGGS.getNPlayers(); i++)
        {
            if(SGGS.getGameScore(i) > currentBestScore)
            {
                winners.clear();
                winners.add(i);
            }
            else if(SGGS.getGameScore(i) == currentBestScore) winners.add(i);
        }

        //More than 1 winner, check pudding amount
        if(winners.size() > 1)
        {
            List<Integer> trueWinners = new ArrayList<>();
            int bestPuddingScore = 0;
            for(int i = 0; i < winners.size(); i++)
            {
                if (GetPuddingAmount(winners.get(i), SGGS) > bestPuddingScore)
                {
                    trueWinners.clear();
                    trueWinners.add(winners.get(i));
                }
                if (GetPuddingAmount(winners.get(i), SGGS) == bestPuddingScore) trueWinners.add(winners.get(i));
            }

            if(trueWinners.size() > 1)
            {
                for (int i = 0; i < SGGS.getNPlayers(); i++)
                {
                    SGGS.setPlayerResult(Utils.GameResult.LOSE, i);
                }
                for (int i = 0; i < trueWinners.size(); i++)
                {
                    SGGS.setPlayerResult(Utils.GameResult.DRAW , trueWinners.get(i));
                }
            }
            else
            {
                for (int i = 0; i < SGGS.getNPlayers(); i++)
                {
                    SGGS.setPlayerResult(Utils.GameResult.LOSE, i);
                }
                SGGS.setPlayerResult(Utils.GameResult.WIN, trueWinners.get(0));
            }
        }
        else
        {
            for (int i = 0; i < SGGS.getNPlayers(); i++)
            {
                SGGS.setPlayerResult(Utils.GameResult.LOSE, i);
            }
            SGGS.setPlayerResult(Utils.GameResult.WIN, winners.get(0));
        }
    }

    private int GetPuddingAmount(int playerId, SGGameState SGGS)
    {
        int amount = 0;
        for (int i = 0; i < SGGS.getPlayerFields().get(playerId).getSize(); i++)
        {
            if(SGGS.getPlayerFields().get(playerId).get(i).type == SGCard.SGCardType.Pudding) amount++;
        }
        return amount;
    }

    private void GiveMakiPoints(SGGameState SGGS)
    {
        //Calculate maki points for each player
        int[] makiPlayerPoints = new int[SGGS.getNPlayers()];
        for (int i = 0; i < SGGS.getNPlayers(); i++)
        {
            for (int j = 0; j < SGGS.getPlayerFields().get(i).getSize(); j++)
            {
                switch (SGGS.getPlayerFields().get(i).get(j).type)
                {
                    case Maki_1:
                        makiPlayerPoints[i] += 1;
                        break;
                    case Maki_2:
                        makiPlayerPoints[i] += 2;
                        break;
                    case Maki_3:
                        makiPlayerPoints[i] += 3;
                        break;
                    default:
                        break;
                }
            }
        }

        //Calculate who has the most points and who has the second most points
        int currentBest = 0;
        int secondBest = 0;
        List<Integer> mostPlayers = new ArrayList<>();
        List<Integer> secondPlayers = new ArrayList<>();
        for(int i = 0; i < makiPlayerPoints.length; i++)
        {
            if(makiPlayerPoints[i] > currentBest)
            {
                secondBest = currentBest;
                secondPlayers.clear();
                for(Integer x : mostPlayers){
                    secondPlayers.add(x.intValue());
                }

                currentBest = makiPlayerPoints[i];
                mostPlayers.clear();
                mostPlayers.add(i);
            }
            else if(makiPlayerPoints[i] == currentBest) mostPlayers.add(i);
            else if(makiPlayerPoints[i] > secondBest)
            {
                secondBest = makiPlayerPoints[i];
                secondPlayers.clear();
                secondPlayers.add(i);
            }
            else if(makiPlayerPoints[i] == secondBest) secondPlayers.add(i);
        }

        //Calculate the score each player gets
        SGParameters parameters = (SGParameters) SGGS.getGameParameters();
        int mostScore = parameters.valueMakiMost;
        int secondScore = parameters.valueMakiSecond;
        if(!mostPlayers.isEmpty()) mostScore /= mostPlayers.size();
        if(!secondPlayers.isEmpty()) secondScore /= secondPlayers.size();

        //Add score to players
        if(currentBest != 0)
        {
            for(int i = 0; i < mostPlayers.size(); i++)
            {
                SGGS.setGameScore(mostPlayers.get(i), (int)SGGS.getGameScore(mostPlayers.get(i)) + mostScore);
            }
        }
        if(secondBest != 0)
        {
            for(int i = 0; i < secondPlayers.size(); i++)
            {
                SGGS.setGameScore(secondPlayers.get(i), (int)SGGS.getGameScore(secondPlayers.get(i)) + secondScore);
            }
        }
    }

    private void GivePuddingPoints(SGGameState SGGS)
    {
        //Calculate maki points for each player
        int[] puddingPlayerPoints = new int[SGGS.getNPlayers()];
        for (int i = 0; i < SGGS.getNPlayers(); i++)
        {
            for (int j = 0; j < SGGS.getPlayerFields().get(i).getSize(); j++)
            {
                switch (SGGS.getPlayerFields().get(i).get(j).type)
                {
                    case Pudding:
                        puddingPlayerPoints[i] += 3;
                        break;
                    default:
                        break;
                }
            }
        }

        //Calculate who has the most points and who has the second most points
        SGParameters parameters = (SGParameters) SGGS.getGameParameters();
        int currentBest = 0;
        int currentWorst = parameters.nPuddingCards + 1;
        List<Integer> mostPlayers = new ArrayList<>();
        List<Integer> leastPlayers = new ArrayList<>();
        for(int i = 0; i < puddingPlayerPoints.length; i++)
        {
            if(puddingPlayerPoints[i] > currentBest)
            {
                currentBest = puddingPlayerPoints[i];
                mostPlayers.clear();
                mostPlayers.add(i);
            }
            else if(puddingPlayerPoints[i] == currentBest) mostPlayers.add(i);
            else currentWorst = puddingPlayerPoints[i];
        }
        if(currentBest > currentWorst)
        {
            for(int i = 0; i < puddingPlayerPoints.length; i++)
            {
                if(puddingPlayerPoints[i] < currentWorst)
                {
                    currentWorst = puddingPlayerPoints[i];
                    leastPlayers.clear();
                    leastPlayers.add(i);
                }
                else if(puddingPlayerPoints[i] == currentWorst) leastPlayers.add(i);
            }
        }

        //Calculate the score each player gets
        int mostScore = parameters.valuePuddingMost;
        int leastScore = parameters.valuePuddingLeast;
        if(!mostPlayers.isEmpty()) mostScore /= mostPlayers.size();
        if(!leastPlayers.isEmpty()) leastScore /= leastPlayers.size();

        //Add score to players
        if(currentBest != 0)
        {
            for(int i = 0; i < mostPlayers.size(); i++)
            {
                SGGS.setGameScore(mostPlayers.get(i), (int)SGGS.getGameScore(mostPlayers.get(i)) + mostScore);
            }
        }
        for(int i = 0; i < leastPlayers.size(); i++)
        {
            SGGS.setGameScore(leastPlayers.get(i), (int)SGGS.getGameScore(leastPlayers.get(i)) + leastScore);
        }
    }

    boolean IsRoundOver(SGGameState SGGS)
    {
        for (int i = 0; i < SGGS.getPlayerDecks().size(); i++)
        {
            if(SGGS.getPlayerDecks().get(i).getSize() > 0) return false;
        }
        return true;
    }

    void RevealCards(SGGameState SGGS)
    {
        for(int i = 0; i < SGGS.getNPlayers(); i++)
        {
            //Moves the card from the players hand to field
            if(SGGS.getPlayerDecks().get(i).getSize() <= SGGS.getPlayerCardPicks()[i] || SGGS.getPlayerCardPicks()[i] < 0) continue;
            SGCard cardToReveal = SGGS.getPlayerDecks().get(i).get(SGGS.getPlayerCardPicks()[i]);
            SGGS.getPlayerDecks().get(i).remove(cardToReveal);
            SGGS.getPlayerFields().get(i).add(cardToReveal);

            if(SGGS.getPlayerChopSticksActivated(i) && SGGS.getPlayerDecks().get(i).getSize() > SGGS.getPlayerExtraCardPicks()[i])
            {
                SGCard extraCardToReveal = SGGS.getPlayerDecks().get(i).get(SGGS.getPlayerExtraCardPicks()[i]);
                SGGS.getPlayerDecks().get(i).remove(extraCardToReveal);
                SGGS.getPlayerFields().get(i).add(extraCardToReveal);
            }

            //Add points to player
            SGGS.setGameScore(i, (int)SGGS.getGameScore(i) + SGGS.getPlayerScoreToAdd(i));
        }
    }



    void RotateDecks(SGGameState SGGS)
    {
        Deck<SGCard> tempDeck;
        tempDeck = SGGS.getPlayerDecks().get(0).copy();
        for(int i = 1; i < SGGS.getNPlayers(); i++)
        {
            SGGS.getPlayerDecks().set(i - 1, SGGS.getPlayerDecks().get(i).copy());
        }
        SGGS.getPlayerDecks().set(SGGS.getNPlayers() - 1, tempDeck.copy());
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        SGGameState SGGS = (SGGameState) gameState;
        ArrayList<AbstractAction> actions = new ArrayList<>();

        int deckFromId = SGGS.getPlayerDecks().get(gameState.getCurrentPlayer()).getComponentID();
        int deckToId = SGGS.getPlayerFields().get(gameState.getCurrentPlayer()).getComponentID();
        Deck<SGCard> currentPlayerHand = SGGS.getPlayerDecks().get(SGGS.getCurrentPlayer());
        for (int i = 0; i < currentPlayerHand.getSize(); i++){
            if(SGGS.getPlayerCardPicks()[SGGS.getCurrentPlayer()] == i) continue;
            switch (currentPlayerHand.get(i).type) {
                case Maki_1:
                    actions.add(new PlayCardAction(SGGS.getCurrentPlayer(), i, SGCard.SGCardType.Maki_1));
                    break;
                case Maki_2:
                    actions.add(new PlayCardAction(SGGS.getCurrentPlayer(), i, SGCard.SGCardType.Maki_2));
                    break;
                case Maki_3:
                    actions.add(new PlayCardAction(SGGS.getCurrentPlayer(), i, SGCard.SGCardType.Maki_3));
                    break;
                case Tempura:
                    actions.add(new PlayCardAction(SGGS.getCurrentPlayer(), i, SGCard.SGCardType.Tempura));
                    break;
                case Sashimi:
                    actions.add(new PlayCardAction(SGGS.getCurrentPlayer(), i, SGCard.SGCardType.Sashimi));
                    break;
                case Dumpling:
                    actions.add(new PlayCardAction(SGGS.getCurrentPlayer(), i, SGCard.SGCardType.Dumpling));
                    break;
                case SquidNigiri:
                    actions.add(new PlayCardAction(SGGS.getCurrentPlayer(), i, SGCard.SGCardType.SquidNigiri));
                    if(SGGS.getPlayerWasabiAvailable(SGGS.getCurrentPlayer()) > 0)
                        actions.add(new NigiriWasabiAction(SGGS.getCurrentPlayer(), i, SGCard.SGCardType.SquidNigiri));
                    break;
                case SalmonNigiri:
                    actions.add(new PlayCardAction(SGGS.getCurrentPlayer(), i, SGCard.SGCardType.SalmonNigiri));
                    if(SGGS.getPlayerWasabiAvailable(SGGS.getCurrentPlayer()) > 0)
                        actions.add(new NigiriWasabiAction(SGGS.getCurrentPlayer(), i, SGCard.SGCardType.SalmonNigiri));
                    break;
                case EggNigiri:
                    actions.add(new PlayCardAction(SGGS.getCurrentPlayer(), i, SGCard.SGCardType.EggNigiri));
                    if(SGGS.getPlayerWasabiAvailable(SGGS.getCurrentPlayer()) > 0)
                        actions.add(new NigiriWasabiAction(SGGS.getCurrentPlayer(), i, SGCard.SGCardType.EggNigiri));
                    break;
                case Wasabi:
                    actions.add(new PlayCardAction(SGGS.getCurrentPlayer(), i, SGCard.SGCardType.Wasabi));
                    break;
                case Chopsticks:
                    actions.add(new PlayCardAction(SGGS.getCurrentPlayer(), i, SGCard.SGCardType.Chopsticks));
                    break;
                case Pudding:
                    actions.add(new PlayCardAction(SGGS.getCurrentPlayer(), i, SGCard.SGCardType.Pudding));
                    break;
            }
        }
        if(SGGS.getPlayerChopSticksAmount(SGGS.getCurrentPlayer()) > 0 && !SGGS.getPlayerChopSticksActivated(SGGS.getCurrentPlayer()))
        {
            actions.add(new ChopSticksAction(SGGS.getCurrentPlayer()));
        }
        if(actions.size() <= 0) actions.add(new DebugAction());
        return actions;
    }

    @Override
    protected AbstractForwardModel _copy() {
        return new SGForwardModel();
    }
}