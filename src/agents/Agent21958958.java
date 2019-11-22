package agents;

import java.util.ArrayList;

import hanabAI.Action;
import hanabAI.ActionType;
import hanabAI.Agent;
import hanabAI.Card;
import hanabAI.Colour;
import hanabAI.IllegalActionException;
import hanabAI.State;

public class Agent21958958 implements Agent {

	//true if it is the first turn of the agent, false otherwise
	private boolean firstAction = true;
	//number of players in the game
	private int numPlayers;
	//number of cards per player
	private int numCards;
	//index of the current player
	private int index;
	//evaluation variable to determine best move, the higher this value, the better the move
	double h;
	//array containing the indexes of the colours hinted to each player since their last move
	private Colour[][] lastHintedColours;
	//array of the colours of the cards still unknown (ie in the deck or in their hand) by the current player, organised by value
	private ArrayList<ArrayList<Colour>> valueCardsLeftArray;
	//array of the values of the cards still unknown (ie in the deck or in their hand) by the current player, organised by colour
	private ArrayList<ArrayList<Integer>> colourCardsLeftArray;
	//number of cards left in the deck
	private int cardsLeft;
	//array containing the index of the cards played from the hands of the other players since the current player's last move (1 if card was played, 0 otherwise)
	private int[][] recentNewCards;
	//array containing the index of the cards in other players' hands in the first move that are not yet dealt with yet (1 if not dealt with, 0 otherwise)
	private int[][] firstRecentNewCards;
	//array containing the colour hints that each player has received
	private Colour[][] playerColours;
	//array containing the value hints that each player has received
	private int[][] playerValues;
	//the hands of the players sorted by the order they arrived in their hands
	private ArrayList<ArrayList<Integer>> playerSortedHands = new ArrayList<>();
	//array listing the colours in alphabetical order for ease of retrieval
	private Colour[] colours = { Colour.BLUE, Colour.GREEN, Colour.RED, Colour.WHITE, Colour.YELLOW };

	/**
	 * Default constructor, does nothing.
	 * **/
	public Agent21958958() {
	}

	/**
	 * Initialises variables on the first call to do action.
	 * @param s the State of the game at the first action
	 **/
	public void init(State s) {
		numPlayers = s.getPlayers().length;

		numCards = (numPlayers > 3 ? 4 : 5);
		playerColours = new Colour[numPlayers][numCards];
		playerValues = new int[numPlayers][numCards];
		lastHintedColours = new Colour[numPlayers][numCards];
		recentNewCards = new int[numPlayers][numCards];
		firstRecentNewCards = new int[numPlayers][numCards];

		cardsLeft = 50 - numPlayers * numCards;
		for (int i = 0; i < numPlayers; i++) {
			playerSortedHands.add(new ArrayList<Integer>());

			for (int j = 0; j < numCards; j++) {
				playerSortedHands.get(i).add(j);
			}
		}

		valueCardsLeftArray = new ArrayList<ArrayList<Colour>>();
		for (int i = 1; i < 6; i++) {
			ArrayList<Colour> temp = new ArrayList<>();
			int num = (i == 1 ? 3 : (i == 5 ? 1 : 2));
			for (int j = 0; j < num; j++) {
				temp.add(Colour.BLUE);
				temp.add(Colour.GREEN);
				temp.add(Colour.RED);
				temp.add(Colour.WHITE);
				temp.add(Colour.YELLOW);
			}
			valueCardsLeftArray.add(temp);
		}

		colourCardsLeftArray = new ArrayList<ArrayList<Integer>>();
		for (int i = 0; i < 5; i++) {
			ArrayList<Integer> temp = new ArrayList<>();
			for (int j = 1; j < 6; j++) {
				for (int k = 0; k < (j == 1 ? 3 : (j == 5 ? 1 : 2)); k++) {
					temp.add(j);
				}
			}
			colourCardsLeftArray.add(temp);
		}

		index = s.getNextPlayer();

	}

	/**
	 * Returns the name BaseLine.
	 * @return the String "BaseLine"
	 * */
	public String toString() {
		return "BaseLine";
	}

	/**
	 * Performs an action given a state.
	 * Assumes that they are the player to move.
	 * The strategy is rule based, and assesses the current game state to determine the next optimal move according to a predefined set of rules
	 * @param s the current state of the game.
	 * @return the action the player takes.
	 **/
	public Action doAction(State s) { //, Card[] hand) {
		if (firstAction) {
			init(s);
		}

		//Assume player's index is s.getNextPlayer()
		index = s.getNextPlayer();

		try {
			//get any hints
			getHints(s);

			//evaluation variable: the better the move, the higher this will be
			h = 0.0;

			//remove all the known cards since the player's last move from the "unknown card" storage arrays 
			removeCards(s);

			return evaluationFunction(s);

		} catch (IllegalActionException e) {
			e.printStackTrace();
			throw new RuntimeException("Something has gone very wrong");
		}
	}

	/**
	 * Updates the colour and value hint arrays from hints received since the player's last move
	 * @param s the current state of the game
	 */
	public void getHints(State s) {
		try {
			//update "unknown cards" arrays for the first player's first move
			if (firstAction && s.getOrder() == 0) {
				for (int k = 0; k < numPlayers; k++) {
					for (int j = 0; j < numCards; j++) {
						if (k != index) {
							firstRecentNewCards[k][j] = 1;
						}
					}
				}
				removeCards(s);
			}
			int max = Math.min(numPlayers, s.getOrder()) - 1;
			for (int i = max; i > -1; i--) {
				State t = (State) s.clone();
				for (int j = 0; j < i; j++) {
					t = t.getPreviousState();
				}
				//update "unknown cards" arrays for the players' first move
				if (firstAction && i == max) {
					for (int k = 0; k < numPlayers; k++) {
						for (int j = 0; j < numCards; j++) {
							if (k != index) {
								firstRecentNewCards[k][j] = 1;
							}
						}
					}
					removeCards(t);
				}

				Action a = t.getPreviousAction();
				if (a.getType() == ActionType.HINT_COLOUR || a.getType() == ActionType.HINT_VALUE) {
					boolean[] hints = t.getPreviousAction().getHintedCards();
					boolean hintToFullyID = false;
					int numHints = 0;
					int colourHintIndex = -1;
					for (int j = 0; j < hints.length; j++) {
						int J = playerSortedHands.get(a.getHintReceiver()).get(j);
						//if hint given, update player hints arrays
						if (hints[J]) {
							if (a.getType() == ActionType.HINT_COLOUR) {
								//if clear colour hint, add that to lastHintedColours
								if (playerColours[a.getHintReceiver()][J] == null) {
									numHints++;
									colourHintIndex = J;
									if (playerValues[a.getHintReceiver()][J] != 0) {
										hintToFullyID = true;
									}
								}
								playerColours[a.getHintReceiver()][J] = a.getColour();
							} else {
								playerValues[a.getHintReceiver()][J] = a.getValue();
							}
						}
					}
					//only add colour hint if it was the only ID'd colour 
					//and the hint wasn't intended to fully ID another card in hand (which already has a number hint)
					if (!hintToFullyID && numHints == 1) {
						lastHintedColours[a.getHintReceiver()][colourHintIndex] = a.getColour();
					}
				} else if (a.getType() == ActionType.PLAY || a.getType() == ActionType.DISCARD) {
					//update sorted hand array
					for (int j = 0; j < numCards; j++) {
						if (playerSortedHands.get(a.getPlayer()).get(j) == a.getCard()) {
							playerSortedHands.get(a.getPlayer()).remove(j);
							playerSortedHands.get(a.getPlayer()).add(a.getCard());
							break;
						}
					}
					//update "unknown card" arrays if the player discarded a card
					if (a.getType() == ActionType.DISCARD && a.getPlayer() == index) {
						one: for (int r = 0; r < 5; r++) {
							if (colours[r] == t.getDiscards().peek().getColour()) {
								for (int u = 0; u < colourCardsLeftArray.get(r).size(); u++) {
									if (colourCardsLeftArray.get(r).get(u) == t.getDiscards().peek().getValue()) {
										colourCardsLeftArray.get(r).remove(u);
										break one;
									}
								}
							}
						}
						for (int r = 0; r < valueCardsLeftArray.get(t.getDiscards().peek().getValue() - 1).size(); r++) {
							if (valueCardsLeftArray.get(t.getDiscards().peek().getValue() - 1).get(r) == t.getDiscards().peek().getColour()) {
								valueCardsLeftArray.get(t.getDiscards().peek().getValue() - 1).remove(r);
								break;
							}
						}
					}
					//update "unknown card" arrays if the player played a card
					if (a.getType() == ActionType.PLAY && a.getPlayer() == index) {
						State u = ((State) t.clone()).getPreviousState();
						for (int d = 0; d < 5; d++) {
							if (playable(u, colours[d]) != playable(t, colours[d])) {
								int play = playable(u, colours[d]);
								for (int e = 0; e < colourCardsLeftArray.get(d).size(); e++) {
									if (colourCardsLeftArray.get(d).get(e) == play) {
										colourCardsLeftArray.get(d).remove(e);
										break;
									}
								}
								for (int r = 0; r < valueCardsLeftArray.get(play - 1).size(); r++) {
									if (valueCardsLeftArray.get(play - 1).get(r) == colours[d]) {
										valueCardsLeftArray.get(play - 1).remove(r);
										break;
									}
								}
							}
						}
					}
					playerColours[a.getPlayer()][a.getCard()] = null;
					playerValues[a.getPlayer()][a.getCard()] = 0;
					cardsLeft--;
					recentNewCards[a.getPlayer()][a.getCard()] = 1;
				}
			}
		} catch (IllegalActionException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Removes the cards from the "unknown card" storage arrays that have been revealed since the player's last turn
	 * @param s the current state of the game
	 */
	public void removeCards(State s) {
		for (int i = 0; i < recentNewCards.length; i++) {
			for (int j = 0; j < recentNewCards[i].length; j++) {
				if ((recentNewCards[i][j] == 1 || firstRecentNewCards[i][j] == 1) && i != index && s.getHand(i)[j] != null) {
					for (int k = 0; k < 5; k++) {
						if (k + 1 == s.getHand(i)[j].getValue()) {
							int valueCardSize = valueCardsLeftArray.get(k).size();
							x: for (int m = 0; m < valueCardSize; m++) {
								if (valueCardsLeftArray.get(k).get(m) == s.getHand(i)[j].getColour()) {
									valueCardsLeftArray.get(k).remove(m);
									break x;
								}
							}
						}

						if (colours[k] == s.getHand(i)[j].getColour()) {
							int colourCardSize = colourCardsLeftArray.get(k).size();
							y: for (int m = 0; m < colourCardSize; m++) {
								if (colourCardsLeftArray.get(k).get(m) == s.getHand(i)[j].getValue()) {
									colourCardsLeftArray.get(k).remove(m);
									recentNewCards[i][j] = 0;
									firstRecentNewCards[i][j] = 0;
									break y;
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Determines which action to take based on the current state of the game a set of pre-determined rules
	 * @param s the current state of the game
	 * @return the action the player will make
	 * @throws IllegalActionException
	 */
	public Action evaluationFunction(State s) throws IllegalActionException {
		Action action = null;
		int nextPlayer = (index + 1) % numPlayers;

		//if it is not the player's final move
		if (cardsLeft > 0) {
			//*********DISCARD**********
			//discard from the cards that have been in the hand the longest
			//giving higher priority to discarding the fewer hints available
			if (s.getHintTokens() == 0) {
				//PRIORITY: 16 or 12 
				Action temp = discardEvaluation(s, 16, 12);
				if (temp != null) { 
					action = temp;
				}

				//if none discarded, discard the most worthless card
				if (h == 0.0) {
					for (int i = 0; i < numCards; i++) {
						int I = playerSortedHands.get(index).get(i);
						int valueHint = playerValues[index][I];
						if (valueHint != 0 && valueCardsLeftArray.get(valueHint - 1).size() != 1) {
							h = 3;
							action = new Action(index, toString(), ActionType.DISCARD, I);
						}
					}
					if (h == 0.0) {
						h = 2;
						action = new Action(index, toString(), ActionType.DISCARD, playerSortedHands.get(index).get(0));
					}
				}
			} else if (s.getHintTokens() == 1) {
				//PRIORITY: 14 or 11
				Action temp = discardEvaluation(s, 14, 11);
				if (temp != null)
					action = temp;
			} else if (s.getHintTokens() == 2) {
				//PRIORITY: 9 or 8
				Action temp = discardEvaluation(s, 9, 8);
				if (temp != null)
					action = temp;
			} else if (s.getHintTokens() < 8) {
				//PRIORIY: 7 or 4
				Action temp = discardEvaluation(s, 7, 4);
				if (temp != null)
					action = temp;
			}

			//***********PLAY************
			for (int i = 0; i < numCards; i++) {
				int I = playerSortedHands.get(index).get(i);
				Colour colourHint = playerColours[index][I];
				int valueHint = playerValues[index][I];

				ArrayList<Integer> colourCards = new ArrayList<>();
				if (colourHint != null)
					colourCards = colourCardsLeft(colourHint);
				ArrayList<Colour> valueCards = new ArrayList<>();
				if (valueHint != 0)
					valueCards = valueCardsLeft(valueHint);
				//PRIORITY: 18
				//play the card IF: 
				//card is known and is playable OR
				//the colour is hinted but not the value and there is only one unknown card of that colour and it is playable OR
				//the value is hinted but not the colour and there is only one unknown card of that value and it is playable OR
				//the value is hinted and it is playable for any possible colours for that value
				if (18 > h) {
					if (colourHint != null && valueHint != 0 && playable(s, colourHint) == valueHint
							|| (colourHint != null && valueHint == 0 && colourCards.size() == 1 && playable(s, colourHint) == colourCards.get(0))
							|| (valueHint != 0 && colourHint == null && ((valueCards.size() == 1 && playable(s, valueCards.get(0)) == valueHint)
									|| allPlayable(s, valueHint, valueCards)))) {
						h = 18;
						action = new Action(index, toString(), ActionType.PLAY, I);
					}
				}

				//PRIORITY: 17
				//play the card IF
				//clear colour hint was given (and if card value is known, check it's playable)
				if (lastHintedColours[index][I] != null && 17 > h
						&& (playerValues[index][I] == 0 || (playerValues[index][I] != 0 && playable(s, playerColours[index][I]) == playerValues[index][I]))) {
					h = 17;
					action = new Action(index, toString(), ActionType.PLAY, I);
				}
			}

			//**********HINT***********
			if (s.getHintTokens() != 0) {
				//PRIORITY: 20
				//IF the first move of the game, number hint the player with the greatest number of ones
				if (s.getOrder() == 0) {
					int[] numOnes = new int[numPlayers];
					for (int i = 0; i < numPlayers; i++) {
						for (int j = 0; j < numCards; j++) {
							if (i != index && s.getHand(i)[j].getValue() == 1) {
								numOnes[i] = numOnes[i] + 1;
							}
						}
					}
					int max = 0;
					int ind = -1;
					for (int i = 0; i < numOnes.length; i++) {
						if (i != index && numOnes[i] > max) {
							max = numOnes[i];
							ind = i;
						}
					}
					if(ind != -1) {						
						h = 20;
						boolean[] val = new boolean[numCards];
						for (int i = 0; i < val.length; i++) {
							val[i] = 1 == s.getHand(ind)[i].getValue();
						}
						action = new Action(index, toString(), ActionType.HINT_VALUE, ind, val, 1);
					}
				}

				//PRIORITY: 19
				//IF next player is about to discard an essential card, number hint that card
				int discard = checkDiscardVital(s, nextPlayer, true);
				if (19 > h) {
					//if a clear hint is already being given to make them play, don't worry about them discarding
					if (!(action != null && action.getType() == ActionType.HINT_COLOUR)) {
						if (discard != -1) {
							h = 19;
							boolean[] val = new boolean[numCards];
							for (int i = 0; i < val.length; i++) {
								val[i] = s.getHand(nextPlayer)[discard].getValue() == s.getHand(nextPlayer)[i].getValue();
							}
							action = new Action(index, toString(), ActionType.HINT_VALUE, nextPlayer, val, s.getHand(nextPlayer)[discard].getValue());
						}
					}
				}

				//PRIORITY: 15
				//IF can hint clearly (ie it is clear that you want them to play that card), hint them
				boolean skipNextPlayer = false;
				//if the next player can play a card they know, skip giving them a hint and give hint to next player
				if(numPlayers != 2) {					
					for (int i = 0; i < numCards; i++) {
						int I = playerSortedHands.get(nextPlayer).get(i);
						if (playerColours[nextPlayer][I] != null && playerValues[nextPlayer][I] != 0
								&& s.getHand(nextPlayer)[I].getValue() == playable(s, s.getHand(nextPlayer)[I].getColour())) {
							skipNextPlayer = true;
						}
					}
				}
				int player = skipNextPlayer ? ((nextPlayer + 1) % numPlayers) : nextPlayer;
				if (15 > h) {
					for (int i = 0; i < numCards; i++) {
						int I = playerSortedHands.get(player).get(i);
						//colour hint the card IF
						//not colour hinted and is playable AND
						//it'll be the only one of that colour to be revealed or it has a number hint
						if (playerColours[player][I] == null
								&& s.getHand(player)[I].getValue() == playable(s, s.getHand(player)[I].getColour())
								&& (playerValues[player][I] != 0
										|| numColouredCards(s, player, s.getHand(player)[I].getColour())
												- numKnownColouredCards(player, s.getHand(player)[I].getColour()) == 1)) {
							h = 15;
							boolean[] col = new boolean[numCards];
							for (int j = 0; j < col.length; j++) {
								col[j] = s.getHand(player)[I].getColour() == s.getHand(player)[j].getColour();
							}
							action = new Action(index, toString(), ActionType.HINT_COLOUR, player, col, s.getHand(player)[I].getColour());
						}

						//number hint the card IF
						//it has a colour hint but no value hint and is playable AND
						//the colour hint isn't a recent one: ie wasn't given to it before they've had a chance to act on it
						if (playerColours[player][I] != null && playerValues[player][I] == 0
								&& playable(s, playerColours[player][I]) == s.getHand(player)[I].getValue() && lastHintedColours[player][I] == null) {
							h = 15;
							boolean[] val = new boolean[numCards];
							for (int j = 0; j < val.length; j++) {
								val[j] = s.getHand(player)[I].getValue() == s.getHand(player)[j].getValue();
							}
							action = new Action(index, toString(), ActionType.HINT_VALUE, player, val, s.getHand(player)[I].getValue());
						}
					}
				}

				//PRIORITY: 10
				//IF not colour hinted and it is playable, colour hint that card
				if (10 > h) {
					for (int i = 0; i < numCards; i++) {
						int I = playerSortedHands.get(nextPlayer).get(i);
						if (playerColours[nextPlayer][I] == null
								&& s.getHand(nextPlayer)[I].getValue() == playable(s, s.getHand(nextPlayer)[I].getColour())) {
							h = 10;
							boolean[] col = new boolean[numCards];
							for (int j = 0; j < col.length; j++) {
								col[j] = s.getHand(nextPlayer)[I].getColour() == s.getHand(nextPlayer)[j].getColour();
							}
							action = new Action(index, toString(), ActionType.HINT_COLOUR, nextPlayer, col, s.getHand(nextPlayer)[I].getColour());
						}
					}
				}

				//PRIORITY: 6
				//IF any player is about to discard a vital card, number hint it
				if (6 > h) {
					for (int i = 1; i < numPlayers; i++) {
						player = (index + i) % numPlayers;
						discard = checkDiscardVital(s, player, true);
						if (discard != -1) {
							h = 6;
							boolean[] val = new boolean[numCards];
							for (int j = 0; j < val.length; j++) {
								val[j] = s.getHand(player)[discard].getValue() == s.getHand(player)[j].getValue();
							}
							action = new Action(index, toString(), ActionType.HINT_VALUE, player, val, s.getHand(player)[discard].getValue());
						}
					}
				}

				//PRIORITY: 13 or 5
				//number hint any player IF
				//only one hint left and about to discard essential (priority 13) OR
				//other hint attempts have failed and they have an essential card in their hand (priority 5)
				if (s.getHintTokens() == 1 || 5 > h) {
					for (int i = 2; i < numPlayers; i++) {
						int playerIndex = (index + i) % numPlayers;
						discard = checkDiscardVital(s, playerIndex, (s.getHintTokens() == 1 && 5 <= h));
						if (!(action != null && action.getType() == ActionType.HINT_COLOUR && discard == action.getHintReceiver() && player == playerIndex)) {
							if (discard != -1) {
								if (13 > h || s.getHintTokens() != 1) {
									h = (s.getHintTokens() == 1? 13 : 5);
									boolean[] val = new boolean[numCards];
									for (int j = 0; j < val.length; j++) {
										val[j] = s.getHand(playerIndex)[discard].getValue() == s.getHand(playerIndex)[j].getValue();
									}
									action = new Action(index, toString(), ActionType.HINT_VALUE, playerIndex, val, s.getHand(playerIndex)[discard].getValue());
								}
							}
						}
					}
				}

				//PRIORITY: 1
				//number hint the card of the next player IF
				//it is the most frequent number in their hand
				if (1 > h) {
					int[] frequency = new int[5];
					for (int i = 0; i < numCards; i++) {
						frequency[s.getHand(nextPlayer)[i].getValue() - 1] += (playerValues[nextPlayer][i] != 0 ? 0 : 1);
					}
					int greatest = 0;
					for (int i = 0; i < 5; i++) {
						if (frequency[i] > greatest)
							greatest = i;
					}
					boolean[] val = new boolean[numCards];
					for (int j = 0; j < val.length; j++) {
						val[j] = greatest == s.getHand(nextPlayer)[j].getValue();
					}
					action = new Action(index, toString(), ActionType.HINT_VALUE, nextPlayer, val, greatest);
				}

			}

			//update sorted hand array
			if (action.getType() == ActionType.PLAY || action.getType() == ActionType.DISCARD) {
				lastHintedColours[index][action.getCard()] = null;
			}
		} else {
			//if the player's last turn
			//*******PLAY******
			for (int i = 0; i < numCards; i++) {
				int I = playerSortedHands.get(index).get(i);
				Colour colourHint = playerColours[index][I];
				int valueHint = playerValues[index][I];

				//PRIORITY: 8 or 5
				//play the card IF card is known and is playable
				if (colourHint != null && valueHint != 0 && playable(s, colourHint) == valueHint) {
					int hValue = ((searchGuaranteed(index, valueHint, colourHint) != null) ? 8 : 5);
					if (hValue > h) {
						h = hValue;
						action = new Action(index, toString(), ActionType.PLAY, I);
					}
				}

				//PRIORITY: 7 or 4
				//play the card IF clear colour hint was given (and if card value is known, check it's playable)
				if (lastHintedColours[index][I] != null
						&& (playerValues[index][I] == 0 || (playerValues[index][I] != 0 && playable(s, playerColours[index][I]) == playerValues[index][I]))) {
					int hValue = ((searchGuaranteed(index, valueHint, colourHint) != null) ? 7 : 4);
					if (hValue > h) {
						h = hValue;
						action = new Action(index, toString(), ActionType.PLAY, I);
					}
				}
			}

			//********HINT********
			if (s.getHintTokens() != 0 && s.getFinalActionIndex() - s.getOrder() >= s.getFuseTokens()) {
				//if can hint so that a later card can be played, do so
				for (int c = 0; c < s.getFinalActionIndex() - s.getOrder(); c++) {
					int i = (index + c + 1) % numPlayers;
					for (int j = 0; j < numCards; j++) {
						int J = playerSortedHands.get(i).get(j);
						//PRIORITY: 6 or 3
						//hint the card IF
						//not colour hinted and it is playable AND 
						//	it'll be the only one of that colour to be revealed or it has a number 
						//OR if it has a colour hint but no value hint
						if ((playerColours[i][J] == null
								&& s.getHand(i)[J].getValue() == playable(s, s.getHand(i)[J].getColour())
								&& (playerValues[i][J] != 0
										|| numColouredCards(s, i, s.getHand(i)[J].getColour())
												- numKnownColouredCards(i, s.getHand(i)[J].getColour()) == 1))
								|| (playerColours[i][J] != null && playerValues[i][J] == 0
										&& playable(s, playerColours[i][J]) == s.getHand(i)[J].getValue())) {
							if (3 > h) {
								if (playerColours[i][J] == null) {
									h = 3;
									boolean[] col = new boolean[numCards];
									for (int k = 0; k < col.length; k++) {
										col[k] = s.getHand(i)[J].getColour() == s.getHand(i)[k].getColour();
									}
									action = new Action(index, toString(), ActionType.HINT_COLOUR, i, col, s.getHand(i)[J].getColour());
								} else {
									h = 3;
									boolean[] val = new boolean[numCards];
									for (int k = 0; k < val.length; k++) {
										val[k] = s.getHand(i)[J].getValue() == s.getHand(i)[k].getValue();
									}
									action = new Action(index, toString(), ActionType.HINT_VALUE, i, val, s.getHand(i)[J].getValue());
								}
							}
							//hint this card IF there is a card in a later players' hand that is playable if this card is played
							if (6 > h) {
								if (c < numPlayers - 1) {
									int[] temp = searchGuaranteed(i, s.getHand(i)[J].getValue(), s.getHand(i)[J].getColour());
									if (temp != null) {
										h = 6;
										if (playerColours[i][J] == null) {
											boolean[] col = new boolean[numCards];
											for (int k = 0; k < col.length; k++) {
												col[k] = s.getHand(i)[J].getColour() == s.getHand(i)[k].getColour();
											}
											action = new Action(index, toString(), ActionType.HINT_COLOUR, i, col, s.getHand(i)[J].getColour());

										} else {
											boolean[] val = new boolean[numCards];
											for (int k = 0; k < val.length; k++) {
												val[k] = s.getHand(i)[J].getValue() == s.getHand(i)[k].getValue();
											}
											action = new Action(index, toString(), ActionType.HINT_VALUE, i, val, s.getHand(i)[J].getValue());
										}
									}
								}
							}
						}
					}
				}
			}

			//PRIORITY: 2
			//if it is safe to play guesses (not enough players to decrease fuse tokens to 0), then try playing unknown card of a known colour
			if (2 > h) {
				for (int i = 0; i < numCards; i++) {
					if (s.getFinalActionIndex() - s.getOrder() + 1 < s.getFuseTokens()
							&& playerColours[index][i] != null && playerValues[index][i] == 0
							&& playable(s, playerColours[index][i]) != -1) {
						h = 2;
						action = new Action(index, toString(), ActionType.PLAY, i);
					}
				}
			}
			//PRIORITY: 1
			//if it is safe to play guesses AND if nothing can be played of a known colour, play randomly
			if (1 > h) {
				for (int i = 0; i < numCards; i++) {
					if (s.getFinalActionIndex() - s.getOrder() + 1 < s.getFuseTokens()
							&& playerValues[index][i] == 0) {
						h = 1;
						action = new Action(index, toString(), ActionType.PLAY, i);
					}
				}
				//last resort, discard a card
				if (1 > h) {
					h = 1;
					action = new Action(index, toString(), ActionType.DISCARD, playerSortedHands.get(index).get(0));
				}
			}
		}

		if (firstAction) {
			firstAction = false;
		}

		return action;
	}

	/**
	 * Returns true if the value given is playable for all the colours given 
	 * @param s the current state
	 * @param value the value to check if they're all playable
	 * @param colours the colours to check the playability of
	 * @return true if the value is playable for all the colours given, false otherwise
	 */
	public boolean allPlayable(State s, int value, ArrayList<Colour> colours) {
		for (int i = 0; i < colours.size(); i++) {
			if (playable(s, colours.get(i)) != value) {
				return false;
			}
		}
		return true;
	}

	/**
	 * For an unknown card with only a number hint, it finds all the possible colours that the given number could be
	 * @param number the hinted number
	 * @return an array of the possible colours that the card could be
	 */
	public ArrayList<Colour> valueCardsLeft(int number) {
		return valueCardsLeftArray.get(number - 1);
	}

	/**
	 * For an unknown card with only a colour hint, it finds all the possible numbers that the card with that number could be
	 * @param colour the hinted colour
	 * @return an array of the possible numbers that the card could be
	 */
	public ArrayList<Integer> colourCardsLeft(Colour colour) {
		int colourIndex = -1;
		for (int i = 0; i < 5; i++) {
			if (colours[i] == colour) {
				colourIndex = i;
				break;
			}
		}
		return colourCardsLeftArray.get(colourIndex);
	}

	/**
	 * Finds the number of cards left of the given colour and number
	 * @param colour the colour of the card
	 * @param number the number of the card
	 * @return an integer which is the number of cards left of the given colour and number
	 */
	public int numCardsLeft(Colour colour, int number) {
		int count = 0;
		for (int i = 0; i < valueCardsLeft(number).size(); i++) {
			if (valueCardsLeft(number).get(i) == colour) {
				count++;
			}
		}
		return count;
	}

	/**
	 * Finds the player and card which can play if the card with the given value and colour is played
	 * @param player the current player playing the card
	 * @param value the value of the card being played
	 * @param colour the colour of the card being played
	 * @return an integer array containing the index of the player which is now able to play and the index of the card they could play, null if there isn't one
	 */
	public int[] searchGuaranteed(int player, int value, Colour colour) {
		for (int i = player + 1; i < numPlayers; i++) {
			for (int j = 0; j < numCards; j++) {
				if (playerValues[i][j] == value + 1 && playerColours[i][j] == colour) {
					return new int[] { i, j };
				}
			}
		}
		return null;
	}

	/**
	 * Returns the index of an essential card in player's hand
	 * (essential meaning there is only one of them left in the game)
	 * 
	 * @param s the current state of the game
	 * @param player the player to check the hand of
	 * @param checkFirst if true, then only check the cards that are about to be discarded, otherwise check all the cards in the hand
	 * @return the index of an essential card in the player's hand, -1 if there isn't one
	 */
	public int checkDiscardVital(State s, int player, boolean checkFirst) {
		boolean safeDiscard = false;
		for (int i = 0; i < numCards; i++) {
			int I = playerSortedHands.get(player).get(i);
			Colour colourHint = playerColours[player][I];
			int valueHint = playerValues[player][I];
			if (colourHint == null && valueHint == 0 && (checkFirst ? !safeDiscard : true)) {
				if (s.getHand(player)[I].getValue() == 5 || playable(s, s.getHand(player)[I].getColour()) == s.getHand(player)[I].getValue()
						|| (numCardsLeft(s.getHand(player)[I].getColour(), s.getHand(player)[I].getValue())
								+ numCardsInHands(s, s.getHand(player)[I].getColour(), s.getHand(player)[I].getValue())) == 1
								&& playable(s, s.getHand(player)[I].getColour()) <= s.getHand(player)[I].getValue()) {
					if ((numCardsLeft(s.getHand(player)[I].getColour(), s.getHand(player)[I].getValue())
							+ numCardsInHands(s, s.getHand(player)[I].getColour(), s.getHand(player)[I].getValue())) == 1) {
					}
					return I;
				}
				safeDiscard = true;
			}
		}
		return -1;
	}

	/**
	 * Returns the number of cards which match the given colour and value in the other players' hands
	 * @param s the current state
	 * @param colour the colour of the cards to find
	 * @param value the value fo the cards to find
	 * @return the number of cards which match the given colour and value in the other players' hands
	 */
	public int numCardsInHands(State s, Colour colour, Integer value) {
		int num = 0;
		for (int i = 0; i < numPlayers; i++) {
			if (i != index) {
				for (int j = 0; j < numCards; j++) {
					if (s.getHand(i)[j].getColour() == colour && s.getHand(i)[j].getValue() == value) {
						num++;
					}
				}
			}
		}
		return num;
	}

	/**
	 * Returns the number of cards in the given player's hand which are of the given colour
	 * @param s the current state
	 * @param player the player to check
	 * @param colour the colour to check
	 * @return the number of cards in the given player's hand which are of the given colour
	 */
	public int numColouredCards(State s, int player, Colour colour) {
		int numColour = 0;
		for (int i = 0; i < numCards; i++) {
			if (s.getHand(player)[i].getColour() != null && s.getHand(player)[i].getColour() == colour) {
				numColour++;
			}
		}
		return numColour;
	}

	/**
	 * Finds the number of cards of the given colour that the player has been colour hinted
	 * @param player the player to check the colour hints of
	 * @param colour the colour of the hint to check
	 * @return the number of cards of the given colour that the player has been colour hinted
	 */
	public int numKnownColouredCards(int player, Colour colour) {
		int numColour = 0;
		for (int i = 0; i < numCards; i++) {
			if (playerColours[player][i] == colour) {
				numColour++;
			}
		}
		return numColour;
	}

	/**
	 * Determines whether or not to discard, depending on the h values given to the method
	 * @param s the current state
	 * @param h1 the h value if the card is fully identified
	 * @param h2 the h value if the card isn't fully identified
	 * @return the discard action
	 * @throws IllegalActionException if the created action is not valid
	 */
	public Action discardEvaluation(State s, int h1, int h2) throws IllegalActionException {
		Action discard = null;
		for (int i = 0; i < numCards; i++) {
			int I = playerSortedHands.get(index).get(i);
			Colour colourHint = playerColours[index][I];
			int valueHint = playerValues[index][I];

			//if card is known exactly and is useless, then discard it 
			if (colourHint != null && valueHint != 0 && playable(s, colourHint) >= valueHint) {
				if (h1 > h) {
					h = h1;
					discard = new Action(index, toString(), ActionType.DISCARD, I);
				}
			}

			//if the value is known and is less than or equal to every firework value already down, discard it
			if (valueHint != 0) {
				boolean ret = true;
				for (int j = 0; j < 5; j++) {
					if (playable(s, colours[j]) <= valueHint) {
						ret = false;
					}
				}
				if (ret) {
					if (h1 > h) {
						h = h1;
						discard = new Action(index, toString(), ActionType.DISCARD, I);
					}
				}
			}

			//if the colour is known and that colour firework is complete, discard it
			if (colourHint != null && playable(s, colourHint) == -1 && h1 > h) {
				h = h1;
				discard = new Action(index, toString(), ActionType.DISCARD, I);
			}

			//if the card is the leftmost card and doesn't have a hint, discard it
			if (colourHint == null && valueHint == 0) {
				if (h2 > h) {
					h = h2;
					discard = new Action(index, toString(), ActionType.DISCARD, I);
				}
			}

			/*//if none discarded and no hints left, discard the leftmost non value hinted card
			if (h == 0.0 && s.getHintTokens() == 0) {
				if (colourHint != null && valueHint == 0) {
					h = 1;
					discard = new Action(index, toString(), ActionType.DISCARD, I);
				}
			}*/
		}
		return discard;
	}

	/**
	 * **Copied from BasicAgent**
	 * Returns the value of the next playable card of the given colour
	 * @param s the current state of the game
	 * @param c the colour to check the next playable card of
	 * @return the value of the next playable card of the given colour
	 */
	public int playable(State s, Colour c) {
		java.util.Stack<Card> fw = s.getFirework(c);
		if (fw.size() == 5)
			return -1;
		else
			return fw.size() + 1;
	}
}
