package operator.recovery;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.jetbrains.annotations.NotNull;
import supervisor.Position;
import utils.Debug;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by higla on 24/02/2018.
 */
public class RecoveryManager {
	
	private String destinationFile;

	public RecoveryManager(String destinationFile)
	{
	    Debug.printVerbose("\n\n\nCreando il file...");
		this.destinationFile=destinationFile;
		this.createNewFile(this.destinationFile);
	}

	private void createNewFile(String destinationFile){

	    File f = new File(destinationFile);
        if(!f.exists())
            try {
                if(f.createNewFile())
                    Debug.printVerbose("File "+ destinationFile+" correctly created");
                else
                    Debug.printError("File "+ destinationFile+" not created");
            } catch (IOException e) {
                Debug.printError(e);
            }
        else
            Debug.printVerbose("File already exists");

    }

	public void appendData(@NotNull DataKey elem)
	{

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
     * @param fileName is the name of the File that will be used for the path
     * @return the value drawn from the list.
     */
    public  DataKey drawDataFromFileList(String fileName){
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
        Type type = new TypeToken<List<DataKey>>() {}.getType();

        try{
            FileWriter writer=new FileWriter(destinationFile, false);
            writer.write( new GsonBuilder().setPrettyPrinting().create().toJson(toStore, type));
            writer.close();
        }
        catch(FileNotFoundException e)
        {
            Debug.printError("File wasn't found!");
        }
        catch(IOException e)
        {
            Debug.printError("No file found!");
        }
    }


    public void removeDataOldestValue()
    {
        if (getAllOrEmptyList().size() > 1)
            drawDataFromFileList(this.destinationFile);
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
        Debug.printVerbose("Those should be eliminated"+toRemove);
        current.removeAll(toRemove);
        store(current);
    }


    /**
     * Return the list of all elements, or null if empty
     */
    private synchronized List<DataKey> getAll()
    {
        String s;
        StringBuilder toConvertFromJSON = new StringBuilder();

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
        } catch (FileNotFoundException e) {
            Debug.printError(e);
        } catch (IOException e) {
            Debug.printError(e);
        }
        finally {
            try {
                fr.close();
            } catch (IOException e) {
                Debug.printError(e);
            }
        }
        return null;

    }

    /**
     * Raturn the content of the file or an empty list
     * @return
     */
    private List<DataKey> getAllOrEmptyList()
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
    public void reactToAck(Key receivedAck, Position ackSenderPosition, Collection<Position> allAcksNeeded)
    {
        List<DataKey> currentlyInFile=getAllOrEmptyList();
        currentlyInFile.stream()
                .filter(datakey->datakey.hasOlderOrEqualSequenceNumberThanOther(receivedAck))
                .filter(correctMessage->!correctMessage.getSources().contains(ackSenderPosition))
                .forEach(messageInNeedOfAck->messageInNeedOfAck.addSource(ackSenderPosition));
        List<DataKey> toRemove=currentlyInFile.stream().filter(datakey->datakey.getSources().size()==allAcksNeeded.size())
                .collect(Collectors.toList());
        currentlyInFile.removeAll(toRemove);
        store(currentlyInFile);
    }
}

