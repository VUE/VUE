package tufts.vue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

public class ObliqueStrategiesDeck extends Hashtable {
	private static final Hashtable captions = new Hashtable();
	private static final int MAX_CARDS = 100;
	
	// Create a list to hold the card numbers
	private List cardnums = new ArrayList();
	// Create a list to hold the cards
	private List cards = new ArrayList();
	
	public ObliqueStrategiesDeck() {
		
		// first, populate our hashtable of captions
		populateCaptions();

		// Now put all the card numbers into the list
		// and shuffle them
		shuffleCardnums();
		
		// create a new deck of card (captions) in shuffle order
		setCardDeck();
				
	}
	
	private void populateCaptions() {
		// start with a clean slate
		captions.clear();
		
		captions.put(1, "Is something too wet, dry?");
		captions.put(2, "Balance");
		captions.put(3, "Is it bankrupt?");
		captions.put(4, "Subtlety- Obscurity-");
		captions.put(5, "Have you boxed yourself in?");
		captions.put(6, "Look for distractions");
		captions.put(7, "Everything comes from its opposite");
		captions.put(8, "Try it again");
		captions.put(9, "How many more times?");
		captions.put(10, "If X was Y");
		captions.put(11, "Re-read the situation");
		captions.put(12, "Inspiration doesn't knock");
		captions.put(13, "Impatience is not a virtue");
		captions.put(14, "It's in you somewhere");
		captions.put(15, "Are you open to suggestion? Listen-");
		captions.put(16, "If it was just a dream");
		captions.put(17, "Bring your feelings to life");
		captions.put(18, "What are you frightened of?");
		captions.put(19, "Are you in the right place?");
		captions.put(20, "Find the truth");
		captions.put(21, "Vast imagination");
		captions.put(22, "Ask your muse");
		captions.put(23, "B before C");
		captions.put(24, "Do the pieces fit together?");
		captions.put(25, "Tie up any loose ends");
		captions.put(26, "I love your work");
		captions.put(27, "Throw it out of kilter");
		captions.put(28, "Texture- Transparency-");
		captions.put(29, "Limit your tools");
		captions.put(30, "Is it unnecessarily long?");
		captions.put(31, "One revolution");
		captions.put(32, "A rainbow of colours");
		captions.put(33, "It's only temporary -\nuntil you make it permanent");
		captions.put(34, "Silence first");
		captions.put(35, "Do what you have to do");
		captions.put(36, "Are you investing your time wisely?");
		captions.put(37, "Is something tying it down?");
		captions.put(38, "Falling out of line");
		captions.put(39, "Different influences for different occasions -\nfind the right one");
		captions.put(40, "Change your frame of mind");
		captions.put(41, "Get a second opinion -\ntake only positive advice");
		captions.put(42, "Reconsider your objective");
		captions.put(43, "Ask someone with fresh ears");
		captions.put(44, "Turn your mind outward");
		captions.put(45, "Make it bleed");
		captions.put(46, "Is there an opportunity here?");
		captions.put(47, "Put the ending out of mind");
		captions.put(48, "Mistakes as creative genius");
		captions.put(49, "Break it down into its elements");
		captions.put(50, "Get uncomfortable");
		captions.put(51, "Are the foundations solid?");
		captions.put(52, "Be original");
		captions.put(53, "Break the rules");
		captions.put(54, "Show off!");
		captions.put(55, "Are you working efficiently?");
		captions.put(56, "Play into your strengths");
		captions.put(57, "Think back to the past - \nare you standing still?");
		captions.put(58, "Close your eyes and feel it");
		captions.put(59, "In a different light");
		captions.put(60, "Make it your own");
		captions.put(61, "What will you make of it years from now?");
		captions.put(62, "Collaborate with a friend");
		captions.put(63, "Paint it black");
		captions.put(64, "Focus on development");
		captions.put(65, "Look for a pattern, rearrange it");
		captions.put(66, "Sleep on it");
		captions.put(67, "Work around its main element");
		captions.put(68, "Detail last");
		captions.put(69, "Work in the future");
		captions.put(70, "Flow");
		captions.put(71, "Merge two different ideas");
		captions.put(72, "Time as currency");
		captions.put(73, "Redefine the problem");
		captions.put(74, "It's a journey");
		captions.put(75, "Recycle");
		captions.put(76, "Make room for new ideas");
		captions.put(77, "Familiar crossroads\nin an unfamiliar journey");
		captions.put(78, "Take only what you need");
		captions.put(79, "Is something subconscious going on?");
		captions.put(80, "Dynamics");
		captions.put(81, "Will others find it interesting?");
		captions.put(82, "It takes a lot of coffee to\nraise the cream to the top");
		captions.put(83, "What jumps out at you?\nWhat did you miss?");
		captions.put(84, "Embrace confusion");
		captions.put(85, "Sound organized in time");
		captions.put(86, "Draw outside the lines");
		captions.put(87, "Go for broke");
		captions.put(88, "Clean lines-\nSmooth edges-");
		captions.put(89, "Focus on potential");
		captions.put(90, "Look for alternate routes");
		captions.put(91, "Save your work as a checkpoint,\nexperiment and be on the lookout\nfor happy accidents");
		captions.put(92, "Camouflage the technicalities");
		captions.put(93, "Fill in the gaps");
		captions.put(94, "Would you do it again?");
		captions.put(95, "Give triviality room to mature");
		captions.put(96, "What is it calling you?");
		captions.put(97, "What are your detractors thinking?");
		captions.put(98, "Consider taking several steps back");
		captions.put(99, "Expand the boundaries");
		captions.put(100, "Yes");		
	}
	
	public List getCardnums() {
		return cardnums;
	}
	
	// wipes the array of card numbers,
	// repopulates and shuffles them
	public void shuffleCardnums() {
		cardnums = new ArrayList();
		
		// Now put all the card numbers into the list
		for (int i=1; i<=MAX_CARDS; i++) {
			cardnums.add(i);
		}
		
		Collections.shuffle(cardnums);
	}
	
	public void setCardDeck() {
		this.clear();
		
		// create a node for each card in shuffle order
		Iterator iter = cardnums.iterator();
		while(iter.hasNext()) {
			int nextcardnum = (Integer) iter.next();
			String strNextLabel = captions.get(nextcardnum).toString();
			//LWNode nextNode = new LWObliqueNode(strNextLabel);
			this.put(nextcardnum, strNextLabel);				
		}
		
	} 
}
