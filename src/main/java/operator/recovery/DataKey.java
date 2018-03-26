package operator.recovery;

import operator.communication.message.MessageData;
import supervisor.Position;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.print.attribute.standard.MediaSize.Other;

import org.jetbrains.annotations.Nullable;

/**
 * Created by higla on 24/02/2018.
 */
public class DataKey implements Serializable{

    private static final long serialVersionUID = -189729754032863167L;
	
	
	private float data;
	/**
	 * id+ sequence number of the node which created the message eg a sum (id 13, s.n. 4) receives 2,4. 
	 * The new data key will bear 13 as id, 5 as sequence number   
	 */
    private Key aggregator;
    private List<Key> sources;
    

    public DataKey(double data,Key aggregator,@Nullable List<Key> sources) {
        this.data = (float)data;
        this.aggregator=aggregator;
        if(sources!=null)
        	this.sources=new ArrayList<>(sources);
    }

    
   

	public float getData() {

        return data;
    }
    //todo: pick one, getData or getValue()
    public float getValue() {

        return data;
    }
    public void setData(int data) {
        this.data = data;
    }




    /**
     * Used in OperatorType to list the not-yet-aggregated information
     * @return id+ sequence number of the node which created the message (see definition)
     */
    public Key getAggregator()
    {
    	return aggregator;
    }




    /**
     * this method checks if
     * @param value is equal to the value
     * @return true if it equal
     */
    private boolean checkEqualValue(float value){
        return (this.data == value);
    }

    /**
     * checks if two
     * @param dataKey s are the same
     * @return true if they are the same
     */
    public boolean checkSameData(DataKey dataKey)
    {
        return dataKey.aggregator==this.aggregator&& checkEqualValue(dataKey.getData());
    }

    @Override
    public String toString() {
        return "DataKey{" +
                "data=" + data +
                ", progressiveKey='" + aggregator + '\'' +
       ", sources='" + sources + '\'' +
       
                '}';
    }
    
    public boolean equals(DataKey key)
    {
    	if(this.aggregator!=null)
    		return this.aggregator.equals(key.aggregator);
    	else return key.aggregator==null;
    }
}
