//description of a memory manager
//in this simple case we assume only one process
//otherwise the we need a list of Page Tables instead of only one
//It is also assumed that frame size is equal to page size
package lab3;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jof
 */
public class MemoryManager {

    private int NbrOfPages; //number of pages in virtual memory 
    private int PageSize; //the number of bytes in one page
    private int NbrOfFrames; //number of frames in physical memory
    private int[] pageTable; //pageTable[n] gives the physical address for page n
    //-1 if page is not in physical memory
    private byte[] RAM; //physical memory RAM
    private RandomAccessFile pageFile;
    private int freePos; //points to the frame where we should insert page
    private int pageFaults = 0;

    private long[] timestamps;
    private int[] physMemory;
    
    private int physicalMemoryFilled;

    // this section sets the size of the pagetable, the amount of pages,
    // the space for the physical memory (RAM) and states the pagefile (.bin file)
    public MemoryManager(int pages, int pageSize, int frames, String pFile) {
        try {
            //initate the virtual memory
            NbrOfPages = pages;
            PageSize = pageSize;
            NbrOfFrames = frames;
            freePos = 0;

            //create pageTable
            //initialy no pages loaded into physical memory
            pageTable = new int[NbrOfPages];
            for (int n = 0; n < NbrOfPages; n++) {
                pageTable[n] = -1;
            }

            //variables for task 3
            timestamps = new long[NbrOfFrames]; //skapa en timestamp för varje frame
            physMemory = new int[NbrOfFrames];
            
            physicalMemoryFilled = 0;
            
            //allocate space for physical memory
            RAM = new byte[NbrOfFrames * PageSize];
            //initiate page file
            pageFile = new RandomAccessFile(pFile, "r");
        } catch (FileNotFoundException ex) {
            Logger.getLogger(MemoryManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public byte read(int logicalAddress) {
        //called by a process to read memory from its logical address
        byte data = 0;
        //calculate pageNumber and index from the logical address

        // variables calculating pagenumber and index
        int pageNumber = (logicalAddress / NbrOfPages);
        int index = (logicalAddress % NbrOfPages);
        
        //check if we get a pageFault
        if (pageTable[pageNumber] == -1) {
            //call method to solve page fault
//            pageFault(pageNumber);
//            pageFaultFIFO(pageNumber);
            pageFaultLRU(pageNumber);
        }
        //read data from RAM
        int frame = pageTable[pageNumber];

        timestamps[frame] = System.nanoTime();

        int physicalAddress = frame * PageSize + index;
        data = RAM[physicalAddress];

        //print result
        System.out.print("Virtual address: " + logicalAddress);
        System.out.print(" Physical address: " + physicalAddress);
        System.out.println(" Value: " + data);
        return data;
    }

    //solve a page fault for page number pageNumber
    private void pageFault(int pageNumber) {
        //this is the simple solution where we assume same size of physical and logical number
        pageFaults++;
        pageTable[pageNumber] = freePos;

        //load page into frame number freePos
        try {
            //read data from pageFile into RAM
            pageFile.seek(pageNumber * PageSize);
            for (int b = 0; b < PageSize; b++) {
                RAM[freePos * PageSize + b] = pageFile.readByte();
            }
        } catch (IOException ex) {
            Logger.getLogger(MemoryManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        //update position to store next page

        freePos++;

    }

    //solve a page fault for page number pageNumber
    private void pageFaultFIFO(int pageNumber) {
        //this solution allows different size of physical and logical number
        //page replacement using FIFO
        //freePos is used to point to next position

        pageFaults++;

        for (int i = 0; i < pageTable.length; i++) {
            if (pageTable[i] == freePos) {
                pageTable[i] = -1;
            }
        }

        pageTable[pageNumber] = freePos;

        try {
            pageFile.seek(pageNumber * PageSize);
            for (int b = 0; b < PageSize; b++) {
                RAM[freePos * PageSize + b] = pageFile.readByte();
            }

        } catch (IOException ex) {
            Logger.getLogger(MemoryManager.class.getName()).log(Level.SEVERE, null, ex);
        }

        //update position to store next page
        freePos++;

        if (freePos == NbrOfFrames) {
            freePos = 0;
        }
    }

    //solve a page fault for page number pageNumber
    private void pageFaultLRU(int pageNumber) {
        //this solution allows different size of physical and logical number
        //victim is chosen by least recently used algorithm

        if (physicalMemoryFilled == 1) {
            int time = 0; // gå igenom alla frames
            for (int i = 0; i < timestamps.length; i++) {
                if (timestamps[time] > timestamps[i]) {
                    time = i; // sätt time till den äldsta 
                }
            }
            pageTable[physMemory[time]] = -1; // sätt pagen som är senast(time) till -1
            freePos = time; //den nya freeposen blir den som är sist använd
        }

        pageFaults++; //faults ++
        pageTable[pageNumber] = freePos;
        physMemory[freePos] = pageNumber; //nuvarande sida blir pagenumber
        timestamps[freePos] = System.nanoTime();

        try {

            pageFile.seek(pageNumber * PageSize);

            for (int b = 0; b < PageSize; b++) {
                RAM[freePos * PageSize + b] = pageFile.readByte();
            }

        } catch (IOException ex) {
            Logger.getLogger(MemoryManager.class.getName()).log(Level.SEVERE, null, ex);
        }

        freePos++;

        if (freePos == NbrOfFrames) {
            physicalMemoryFilled = 1; // kolla om den är full
        }
        
    }

    public int getNbrOfPagefaults() {
        return pageFaults;
    }
}
