import sun.invoke.empty.Empty;

/**
 * Class BlockStack
 * Implements character block stack and operations upon it.
 *
 * $Revision: 1.4 $
 * $Last Revision Date: 2019/02/02 $
 *
 * @author Serguei A. Mokhov, mokhov@cs.concordia.ca;
 * Inspired by an earlier code by Prof. D. Probst

 */
class BlockStack
{
	/**
	 * # of letters in the English alphabet + 2
	 */
	public static final int MAX_SIZE = 28;

	/**
	 * Default stack size
	 */
	public static final int DEFAULT_SIZE = 6;

	/**
	 * Current size of the stack
	 */
	private int iSize = DEFAULT_SIZE;

	/**
	 * Current top of the stack
	 */
	private int iTop  = 3;

	/**
	 * stack[0:5] with four defined values
	 */
	private char acStack[] = new char[] {'a', 'b', 'c', 'd', '*', '*'};

	/**
	 * Number of times the stack has been accessed since
	 * the program has started execution
	 * (Incremented by 1 every single time the stack is accessed)
	 */
	private int accessCounter = 0;

	/**
	 * Default constructor
	 */
	public BlockStack()
	{
	}

	/**
	 * Supplied size
	 * @throws InvalidStackSizeException
	 */
	public BlockStack(final int piSize)
        throws InvalidStackSizeException
	{
	    if (piSize < 0 || piSize > MAX_SIZE)
	        throw new InvalidStackSizeException();
		if(piSize != DEFAULT_SIZE)
		{
			this.acStack = new char[piSize];

			// Fill in with letters of the alphabet and keep
			// 2 free blocks
			for(int i = 0; i < piSize - 2; i++)
				this.acStack[i] = (char)('a' + i);

			this.acStack[piSize - 2] = this.acStack[piSize - 1] = '*';

			this.iTop = piSize - 3;
			this.iSize = piSize;
		}
	}

	/**
	 * Picks a value from the top without modifying the stack
	 * @return top element of the stack, char
	 * @throws EmptyStackException
	 */
	public char pick()
		throws EmptyStackException
	{
		if (this.isEmpty())
			throw new EmptyStackException();
		this.accessCounter++;
		return this.acStack[this.iTop];
	}

	/**
	 * Returns arbitrary value from the stack array
	 * @return the element, char
	 * @throws OutOfBoundsStackIndexException
	 */
	public char getAt(final int piPosition)
		throws OutOfBoundsStackIndexException
	{
		if (piPosition < 0 || piPosition >= this.iSize)
			throw new OutOfBoundsStackIndexException();
		this.accessCounter++;
		return this.acStack[piPosition];
	}

	/**
	 * Standard push operation
	 * @throws EmptyStackException
	 */
	public void push(final char pcBlock)
        throws FullStackException
    {
        if (this.isFull())
            throw new FullStackException();
        if (this.isEmpty()) {
            this.acStack[++this.iTop] = 'a';
        }
	    else {
            this.acStack[++this.iTop] = pcBlock;
        }
		this.accessCounter++;
		System.out.println("Element " + pcBlock + " has successfully been pushed to the stack.");
	}

	/**
	 * Standard pop operation
	 * @return ex-top element of the stack, char
	 * @throws EmptyStackException
	 */
	public char pop()
        throws EmptyStackException
	{
	    if (this.isEmpty())
	        throw new EmptyStackException();
		char cBlock = this.acStack[this.iTop];
		this.acStack[this.iTop--] = '*'; // Leave prev. value undefined
		this.accessCounter++;
		System.out.println("Element " + cBlock + " has successfully been popped (removed) from the stack.");
		return cBlock;
	}

	/**
	 * We use variable iTop to determine the emptiness of the stack.
	 * @return whether or not the stack is empty
	 */
	public boolean isEmpty()
	{
		return this.iTop == -1;
	}

    /**
     * We use variable iTop to determine the fullness of the stack relative to iSize.
     * @return whether or not the stack is full
     */
    public boolean isFull()
    {
        return this.iTop == this.iSize;
    }

	/*------- Accessor Methods -------*/
	/**
	 * @return Index of the element currently on top of the stack
	 */
	public int getITop()
	{
		return iTop;
	}

	/**
	 * @return Current size of the stack
	 */
	public int getISize()
	{
		return iSize;
	}

	/**
	 * @return accessCounter, that counts the number of times stack has been accessed since we started execution.
	 */
	public int getAccessCounter()
	{
		return accessCounter;
	}

	/**
	 * @return the stack itself (implemented as a 1-dimensional array of characters)
	 */
	public char[] getAcStack()
	{
		return acStack;
	}
}

/**
 * Exception thrown when an invalid/forbidden operation
 * has been requested on an empty stack.
 */
class EmptyStackException extends Exception
{
    public EmptyStackException()
    {
        super("Empty Stack !!!");
    }
    public EmptyStackException(String message)
    {
        super(message);
    }
}

/**
 * Exception thrown when an invalid/forbidden operation
 * has been requested on a full stack.
 */
class FullStackException extends Exception
{
    public FullStackException()
    {
        super("Full Stack !!!");
    }
    public FullStackException(String message)
    {
        super(message);
    }
}

/**
 * Exception thrown when the stack size requested is invalid,
 * i.e. either negative or greater than the defined MAX_SIZE.
 */
class InvalidStackSizeException extends Exception
{
    public InvalidStackSizeException()
    {
        super("Invalid Stack Size !!!");
    }
    public InvalidStackSizeException(String message)
    {
        super(message);
    }
}

/**
 * Exception thrown when the requested index is outside the valid elements of the stack
 * i.e. either negative or greater than iTop (greater than the top of the stack).
 */
class OutOfBoundsStackIndexException extends Exception
{
	public OutOfBoundsStackIndexException()
	{
		super("Invalid Stack Index !!! (index is a value outside of the current stack size)");
	}
	public OutOfBoundsStackIndexException(String message)
	{
		super(message);
	}
}

// EOF
