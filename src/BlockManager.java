// Import (aka include) some stuff.
import common.*;
import org.omg.CORBA.DynAnyPackage.Invalid;
import sun.invoke.empty.Empty;

/**
 * Class BlockManager
 * Implements character block "manager" and does twists with threads.
 *
 * @author Serguei A. Mokhov, mokhov@cs.concordia.ca;
 * Inspired by previous code by Prof. D. Probst
 *
 * $Revision: 1.5 $
 * $Last Revision Date: 2019/02/02 $

 */
public class BlockManager
{
	/**
	 * The stack itself
	 */
	private static BlockStack soStack = new BlockStack();

	/**
	 * Number of threads dumping stack
	 */
	private static final int NUM_PROBERS = 4;

	/**
	 * Number of steps they take
	 */
	private static int siThreadSteps = 5;

	/**
	 * For atomicity
	 */
	private static Semaphore mutex = new Semaphore(1);

	/**
	 * For phase 1, we need to know how many threads will be created.
	 * There are 3 types of threads: AcquireBlock, ReleaseBlock and CharStackProber threads,
	 * who all derive from common/BaseThread, which itself is a class deriving from the Java Thread class.
	 * Thus, we need to count the total number of threads, which is the sum of the number of threads of each class.
	 * In our case, 3 + 3 + 4 = 10 threads in execution.
	 */
	private static final int NUM_ACQUIRE_BLOCK_THREADS = 3;
	private static final int NUM_RELEASE_BLOCK_THREADS = 3;
	private static int nbrTotalThreads = NUM_ACQUIRE_BLOCK_THREADS + NUM_RELEASE_BLOCK_THREADS + NUM_PROBERS; // = 10 threads

	/*
	 * To indicate to the user that all threads have finished executing phase 1 and are ready to execute phase 2.
	 */
	private static boolean phase1FullyCompleted = false;

	/*
	 * For synchronization
	 */

	/**
	 * Semaphore s1 is necessary to make sure that all threads have finished executing their phase I
	 * before phase II starts executing for any threads.
	 * To ensure that, we initialize s1 to the negative value of the total number of threads incremented by 1.
	 * Thus, all threads will subsequently be forced to wait until the value of s1 becomes positive,
	 * which will only happen when all threads have finished executing Phase 1.
	 * In that way, as soon as the last thread (10th one here) has finished executing Phase 1,
	 * the value of s1 will be 1, allowing any thread to enter/start executing phase 2.
	 */
	private static Semaphore s1 = new Semaphore(-nbrTotalThreads + 1);

	/**
	 * s2 is for use in conjunction with Thread.turnTestAndSet() for phase II proceed
	 * in the thread creation order
	 */
	//private static Semaphore s2 = new Semaphore(...);


	// The main()
	public static void main(String[] argv)
	{
		try {
			// Some initial stats...
			System.out.println("Main thread starts executing.");
			System.out.println("Initial value of top = " + soStack.getITop() + ".");
			System.out.println("Initial value of stack top = " + soStack.pick() + ".");
			System.out.println("Main thread will now fork several threads.");

			/*
			 * The birth of threads
			 */
			AcquireBlock ab1 = new AcquireBlock();
			AcquireBlock ab2 = new AcquireBlock();
			AcquireBlock ab3 = new AcquireBlock();

			System.out.println("main(): Three AcquireBlock threads have been created.");

			ReleaseBlock rb1 = new ReleaseBlock();
			ReleaseBlock rb2 = new ReleaseBlock();
			ReleaseBlock rb3 = new ReleaseBlock();

			System.out.println("main(): Three ReleaseBlock threads have been created.");

			// Create an array object first
			CharStackProber aStackProbers[] = new CharStackProber[NUM_PROBERS];

			// Then the CharStackProber objects
			for (int i = 0; i < NUM_PROBERS; i++)
				aStackProbers[i] = new CharStackProber();

			System.out.println("main(): CharStackProber threads have been created: " + NUM_PROBERS);

			/*
			 * Twist 'em all
			 */
			ab1.start();
			aStackProbers[0].start();
			rb1.start();
			aStackProbers[1].start();
			ab2.start();
			aStackProbers[2].start();
			rb2.start();
			ab3.start();
			aStackProbers[3].start();
			rb3.start();

			System.out.println("main(): All the threads are ready.");

			/*
			 * Wait by here for all forked threads to die
			 */
			ab1.join();
			ab2.join();
			ab3.join();

			rb1.join();
			rb2.join();
			rb3.join();

			for (int i = 0; i < NUM_PROBERS; i++)
				aStackProbers[i].join();

			// Some final stats after all the child threads terminated...
			System.out.println("System terminates normally.");
			System.out.println("Final value of top = " + soStack.getITop() + ".");
			System.out.println("Final value of stack top = " + soStack.pick() + ".");
			System.out.println("Final value of stack top-1 = " + soStack.getAt(soStack.getITop() - 1) + ".");
			System.out.println("Stack access count = " + soStack.getAccessCounter());

			System.exit(0);
		}
		catch(EmptyStackException e)
		{
			System.err.println("Caught EmptyStackException: " + e.getMessage());
			System.exit(1);
		}
		catch(OutOfBoundsStackIndexException e)
		{
			System.err.println("Caught OutOfBoundsStackIndexException: " + e.getMessage());
			System.exit(1);
		}
		catch(InterruptedException e)
		{
			System.err.println("Caught InterruptedException (internal error): " + e.getMessage());
			e.printStackTrace(System.err);
		}
		catch(Exception e)
		{
			reportException(e);
		}
		finally
		{
			System.exit(1);
		}
	} // main()


	/**
	 * Inner AcquireBlock thread class.
	 */
	static class AcquireBlock extends BaseThread
	{
		/**
		 * A copy of a block returned by pop().
		 * @see BlockStack#pop()
		 */
		private char cCopy;

		public void run()
		{
			System.out.println("AcquireBlock thread [TID=" + this.iTID + "] starts executing.");


			phase1();

			// The following try catch block contains operations that access and modify the stack, which is a shared
			// resource and more importantly a critical section that needs to be protected. Thus, we use the mutex
			// to guarantee mutual exclusivity.
			mutex.P();

			// s1 is initialized to -nbrTotalThreads + 1, (which in this specific case is -9). Thus, after a thread
			// completes Phase 1, we increment that value. It will stay negative, preventing any thread to enter
			// Phase 2 right until the moment the very last remaining thread (in this case thread 10th) finishes
			// Phase 1 and arrives to the following line, when the value of s1 will become 1.
			s1.V();

			// Once the very last thread (in our case the 10th one) finishes executing Phase 1, it should unlock the
			// semaphore s1. The following conditional checks for that and notifies the user of such accomplishment.
			if (!s1.isLocked())
			{
				System.out.println("READY FOR PHASE 2 => All " + nbrTotalThreads +
						" threads have finished executing Phase 1 successfully!!!");
			}

			try
			{
				System.out.println("AcquireBlock thread [TID=" + this.iTID + "] requests Ms block.");

				this.cCopy = soStack.pop();

				System.out.println
						(
								"AcquireBlock thread [TID=" + this.iTID + "] has obtained Ms block " + this.cCopy +
										" from position " + (soStack.getITop() + 1) + "."
						);


				System.out.println
						(
								"Acq[TID=" + this.iTID + "]: Current value of top = " +
										soStack.getITop() + "."
						);

				System.out.println
						(
								"Acq[TID=" + this.iTID + "]: Current value of stack top = " +
										soStack.pick() + "."
						);
			}
			catch(EmptyStackException e)
			{
				System.err.println("Caught EmptyStackException: " + e.getMessage());
				System.exit(1);
			}
			catch(Exception e)
			{
				reportException(e);
				System.exit(1);
			}
			finally {
				// In all cases, even if some exceptions occur, we make sure to release the mutex we acquired.
				mutex.V();
			}

			// The following line forces all threads to wait until every single thread has finished executing Phase 1
			// before starting to execute Phase 2 (assuming s1 has been initialized to -totalNbrThreads + 1).
			// Phase 2 will only be able to be executed once the value of s1
			// becomes strictly positive. Once that happens, all threads should be able to start executing Phase 2.
			s1.P();
			s1.V();
			phase2();

			System.out.println("AcquireBlock thread [TID=" + this.iTID + "] terminates.");
		}
	} // class AcquireBlock


	/**
	 * Inner class ReleaseBlock.
	 */
	static class ReleaseBlock extends BaseThread
	{
		/**
		 * Block to be returned. Default is 'a' if the stack is empty.
		 */
		private char cBlock = 'a';

		public void run()
		{
			System.out.println("ReleaseBlock thread [TID=" + this.iTID + "] starts executing.");


			phase1();

			// The following try catch block contains operations that access and modify the stack, which is a shared
			// resource and more importantly a critical section that needs to be protected. Thus, we use the mutex
			// to guarantee mutual exclusivity.
			mutex.P();

			// s1 is initialized to -nbrTotalThreads + 1, (which in this specific case is -9). Thus, after a thread
			// completes Phase 1, we increment that value. It will stay negative, preventing any thread to enter
			// Phase 2 right until the moment the very last remaining thread (in this case thread 10th) finishes
			// Phase 1 and arrives to the following line, when the value of s1 will become 1.
			s1.V();

			// Once the very last thread (in our case the 10th one) finishes executing Phase 1, it should unlock the
			// semaphore s1. The following conditional checks for that and notifies the user of such accomplishment.
			if (!s1.isLocked())
			{
				System.out.println("READY FOR PHASE 2 => All " + nbrTotalThreads +
						" threads have finished executing Phase 1 successfully!!!");
			}

			try
			{
				if(soStack.isEmpty() == false)
					this.cBlock = (char)(soStack.pick() + 1);


				System.out.println
						(
								"ReleaseBlock thread [TID=" + this.iTID + "] returns Ms block " + this.cBlock +
										" to position " + (soStack.getITop() + 1) + "."
						);

				soStack.push(this.cBlock);

				System.out.println
						(
								"Rel[TID=" + this.iTID + "]: Current value of top = " +
										soStack.getITop() + "."
						);

				System.out.println
						(
								"Rel[TID=" + this.iTID + "]: Current value of stack top = " +
										soStack.pick() + "."
						);
			}
			catch(FullStackException e)
			{
				System.err.println("Caught FullStackException: " + e.getMessage());
				System.exit(1);
			}
			catch(EmptyStackException e)
			{
				System.err.println("Caught EmptyStackException: " + e.getMessage());
				System.exit(1);
			}
			catch(Exception e)
			{
				reportException(e);
				System.exit(1);
			}
			finally {
				// In all cases, even if some exceptions occur, we make sure to release the mutex we acquired.
				mutex.V();
			}

			// The following line forces all threads to wait until every single thread has finished executing Phase 1
			// before starting to execute Phase 2 (assuming s1 has been initialized to -totalNbrThreads + 1).
			// Phase 2 will only be able to be executed once the value of s1
			// becomes strictly positive. Once that happens, all threads should be able to start executing Phase 2.
			s1.P();
			s1.V();
			phase2();

			System.out.println("ReleaseBlock thread [TID=" + this.iTID + "] terminates.");
		}
	} // class ReleaseBlock


	/**
	 * Inner class CharStackProber to dump stack contents.
	 */
	static class CharStackProber extends BaseThread
	{
		public void run()
		{

			// When dumping stack contents,
			// only one thread should be operational at a time, thus we need to acquire the mutex.
			// This is because we execute operations on the stack, which should be mutually exclusive.
			// (Since the stack is a critical section.)
			phase1();

			// The following try catch block contains operations that access and modify the stack, which is a shared
			// resource and more importantly a critical section that needs to be protected. Thus, we use the mutex
			// to guarantee mutual exclusivity.
			mutex.P();

			// s1 is initialized to -nbrTotalThreads + 1, (which in this specific case is -9). Thus, after a thread
			// completes Phase 1, we increment that value. It will stay negative, preventing any thread to enter
			// Phase 2 right until the moment the very last remaining thread (in this case thread 10th) finishes
			// Phase 1 and arrives to the following line, when the value of s1 will become 1.
			s1.V();
			// Once the very last thread (in our case the 10th one) finishes executing Phase 1, it should unlock the
			// semaphore s1. The following conditional checks for that and notifies the user of such accomplishment.
			if (!s1.isLocked())
			{
				System.out.println("READY FOR PHASE 2 => All " + nbrTotalThreads +
						" threads have finished executing Phase 1 successfully!!!");
			}

			try
			{
				for(int i = 0; i < siThreadSteps; i++)
				{
					System.out.print("Stack Prober [TID=" + this.iTID + "]: Stack state: ");

					// [s] - means ordinay slot of a stack
					// (s) - current top of the stack
					for(int s = 0; s < soStack.getISize(); s++)
						System.out.print
								(
										(s == BlockManager.soStack.getITop() ? "(" : "[") +
												BlockManager.soStack.getAt(s) +
												(s == BlockManager.soStack.getITop() ? ")" : "]")
								);

					System.out.println(".");

				}
			}
			catch(OutOfBoundsStackIndexException e)
			{
				System.err.println("Caught OutOfBoundsStackIndexException: " + e.getMessage());
				System.exit(1);
			}
			catch(Exception e)
			{
				reportException(e);
				System.exit(1);
			}
			finally {
				// In all cases, even if some exceptions occur, we make sure to release the mutex we acquired.
				mutex.V();
			}

			// The following line forces all threads to wait until every single thread has finished executing Phase 1
			// before starting to execute Phase 2 (assuming s1 has been initialized to -totalNbrThreads + 1).
			// Phase 2 will only be able to be executed once the value of s1
			// becomes strictly positive. Once that happens, all threads should be able to start executing Phase 2.
			s1.P();
			s1.V();
			phase2();
		}
	} // class CharStackProber


	/**
	 * Outputs exception information to STDERR
	 * @param poException Exception object to dump to STDERR
	 */
	private static void reportException(Exception poException)
	{
		System.err.println("Caught exception : " + poException.getClass().getName());
		System.err.println("Message          : " + poException.getMessage());
		System.err.println("Stack Trace      : ");
		poException.printStackTrace(System.err);
	}
} // class BlockManager

// EOF
