package operator.recovery;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import supervisor.Position;
import utils.Debug;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by higla on 24/02/2018.
 */
public class RecoveryManager {
	
	private String destinationFile;

	public RecoveryManager(String destinationFile)
	{
        Logger logger = LogManager.getLogger();
        logger.debug("\n\n\nCreando il file...");
		this.destinationFile=destinationFile;
		this.createNewFile(this.destinationFile);
	}

	private void createNewFile(String destinationFile){

	    File f = new File(destinationFile);

        Logger logger = LogManager.getLogger();

        if(!f.exists())
            try {
                if(f.createNewFile())
                    logger.debug("File "+ destinationFile+" correctly created");
                else
                    logger.debug("File "+ destinationFile+" not created");
            } catch (IOException e) {
                logger.error(e);
            }
        else
            logger.debug("File already exists");

    }

	public void appendData(@NotNull DataKey elem)
	{
        Logger logger = LogManager.getLogger();
        logger.debug("Printing " + elem.toString() + "in file " + destinationFile);
		appendDataInFileList(destinationFile, elem);
	}

	
    /**
     * this method appends to an existing file, new Datas.
     * If the file doesn't exist it creates one. Note that if the file already exists (and it has old data),
     * the method will append datas anyway. So first it's better to use a safer method that creates a new file
     * anyway, then use this method.
     * This method also checks if the data is already there. If it is, data is not added.
     * @param fileName is the name of the File that will be used for the path
     * @param dataKey is the value to append to the file
     */
    private synchronized void appendDataInFileList(String fileName, DataKey dataKey){
        List<DataKey> datas = getAll();
        if(datas != null) {
            //i check if dataKey isn't already in the file
            for (DataKey data : datas) {
                if(data.equals(dataKey)){
                    return;
                }
            }
            datas.add(dataKey);
        }
        else{
            datas = new ArrayList<>();
            datas.add(dataKey);
        }
        store(datas);
    }

    /**
     * This method draws a value from a file and returns it. It also overwrites the file with the new list.
     * @return the value drawn from the list.
     */
    public  DataKey drawDataFromFileList(){
        DataKey value = null;

            Type type = new TypeToken<List<DataKey>>() {}.getType();
            List<DataKey> datas = getAll();

        if(datas != null) {
                value = datas.get(0);
                datas.remove(value);
            }
            store(datas);

        return value;


    }

    public void removeDataFromList(DataKey dataToCheck){

            List<DataKey> datas = getAll();
            if(datas != null) {
                //i look for the data
                List<DataKey> toRemove;
                toRemove=datas.stream().filter(dat->dat.checkSameData(dataToCheck)).collect(Collectors.toList());
                datas.removeAll(toRemove);
            }
            store(datas);

    }

    /**
     * Save to file the given list of elements
     * @param toStore
     */
    public synchronized void store(List<DataKey> toStore)
    {
        Debug.printError("Sto stampando sul file " +  destinationFile.toString() + "\n");
        for(DataKey message:toStore)
        {
            Debug.printError(toStore + " ");
        }
        Debug.printError("\n");

        Type type = new TypeToken<List<DataKey>>() {}.getType();
        Logger logger = LogManager.getLogger();
        try{
            FileWriter writer=new FileWriter(destinationFile, false);
            writer.write( new GsonBuilder().setPrettyPrinting().create().toJson(toStore, type));
            writer.close();
        }
        catch(FileNotFoundException e)
        {
            logger.error("File wasn't found!");
        }
        catch(IOException e)
        {
            logger.error(e);
        }
    }


    /**
     * Remove from the file the value with same original key and lowest sequence number
     * @param originalKey
     */
    public void removeDataOldestValueByKey(DataKey originalKey)
    {
    	List<DataKey> all=getAllOrEmptyList();
    	DataKey toDelete=originalKey.oldestInListWithSameOriginalKey(all);
    	all.remove(toDelete);
    	store(all);
    }

   
    
    
    /**
     * Remove from file all values older than anyone in the given list.
     * @param sources
     */
    public void keepOnlyTheMostRecentForEachSource(@NotNull List<DataKey> sources)
    {
        List<DataKey> current=this.getAllOrEmptyList();

        current.addAll(sources);
        
        
        List<DataKey> toRemove=current.stream()
                .filter(currentlyInFileOrNewer->current.stream()
                        .anyMatch(moreRecent->moreRecent.otherHasSameSenderButOlderSequenceNumber(currentlyInFileOrNewer)))
                .collect(Collectors.toList());
        Debug.printVerbose("Those should be eliminated because i'm the last message for each source"+toRemove);
        current.removeAll(toRemove);
        store(current);
    }


    /**
     * Return the list of all elements, or null if empty
     */
    public synchronized @Nullable List<DataKey> getAll()
    {
        String s;
        StringBuilder toConvertFromJSON = new StringBuilder();
        Logger logger = LogManager.getLogger();

        FileReader fr=null;
        try {
            fr = new FileReader(destinationFile);
            BufferedReader br=new BufferedReader(fr);

            while((s=br.readLine())!=null)
            {
                toConvertFromJSON.append(s);

            }
            Type type = new TypeToken<List<DataKey>>() {}.getType();
            List<DataKey> datas = new Gson().fromJson(toConvertFromJSON.toString(), type);
            if(datas!=null)
                return datas;
        } catch (IOException e) {
            logger.error(e);
        }
        finally {
            try {
                fr.close();
            } catch (IOException e) {
                logger.error(e);
            }
        }
        return null;

    }

    /**
     * Raturn the content of the file or an empty list
     * @return
     */
    public List<DataKey> getAllOrEmptyList()
    {
        List<DataKey> toRet=getAll();
        return toRet==null?new ArrayList<>():toRet;
    }

    /**
     * Store into file the information that the message identified by receivedAck was received by the node located at
     * acksenderposition.
     * Moreover, all previous messages directed to the same node are implicitly acknowledged.
     * @param receivedAck
     * @param ackSenderPosition
     * @param allAcksNeeded
     */
    public synchronized void reactToAck(String originalKey,Key receivedAck, Position ackSenderPosition, Collection<Position> allAcksNeeded)
    {
        List<DataKey> currentlyInFile=getAllOrEmptyList();
        /*currentlyInFile.stream()
        		.filter(msg->msg.getOriginalKey().equals(originalKey))
                .filter(datakey->datakey.hasOlderOrEqualSequenceNumberThanOther(receivedAck))
                .filter(correctMessage->!correctMessage.getSources().contains(ackSenderPosition))
                .forEach(messageInNeedOfAck->messageInNeedOfAck.addSource(ackSenderPosition));
        List<DataKey> toRemove=currentlyInFile.stream().filter(datakey->datakey.getSources().size()==allAcksNeeded.size())
                .collect(Collectors.toList());*/
        Logger logger = LogManager.getLogger();

        for(DataKey key:currentlyInFile)
            logger.debug("Pre filtrando... " + key.toString());

        filterAck(originalKey, receivedAck, ackSenderPosition,currentlyInFile);

        for(DataKey key:currentlyInFile)
            logger.debug("Filtrando... " + key.toString() + "  con ack " + receivedAck.toString());

        allAcksNeeded.forEach(
                d-> logger.debug(d.toString())
        );

        currentlyInFile.removeAll(returnAckedMsgs(currentlyInFile, allAcksNeeded))
;
        store(currentlyInFile);
    }

    /**
     *
     * @param originalKey
     * @param receivedAck
     * @param ackSenderPosition
     * @param messagesToFilter adds to all messages of the list, an ack from ackSenderPosition
     */
    public static void filterAck(String originalKey, Key receivedAck, Position ackSenderPosition,
                                    List<DataKey> messagesToFilter){
        List<DataKey> sameKey  = new LinkedList<>();

        for(DataKey message: messagesToFilter)
        {
            //here we filter the message we just have to ack
            if(message.getOriginalKey().equals(originalKey) && message.getAggregator().getSequenceNumber()<=
                    receivedAck.getSequenceNumber()){
                sameKey.add(message);
            }
        }

        for(DataKey message:sameKey)
        {
            message.getAcked(ackSenderPosition);
        }
        return;
    }

    public static List<DataKey> returnAckedMsgs(List<DataKey> messages, Collection<Position> positions){
        List<DataKey> toReturn = new LinkedList<>();
        for(DataKey message:messages)
        {
            if(message.getSources().size() == positions.size()){
                toReturn.add(message);
            }
        }
        return toReturn;
    }

    public List<DataKey> getByKey(DataKey key)
    {
    	List<DataKey> ret=getAllOrEmptyList();
    	return ret.stream().filter(p->p.getOriginalKey().equals(key.getOriginalKey())).collect(Collectors.toList());
    }
    
    /**
     * Check if a message with same key and bigger sequence number exists
     * @param messageData
     * @return
     */
	/*public boolean isDuplicated(DataKey messageData)
	{
		return getByKey(messageData).stream().anyMatch(m->m.getAggregator().getSequenceNumber()>=messageData.getAggregator().getSequenceNumber());
	}
    */


    public boolean isDuplicated(DataKey messageData)
    {
        return getByKey(messageData).stream().anyMatch(m->m.otherHasSameSenderButOlderOrSameSequenceNumber(messageData));
    }
}

