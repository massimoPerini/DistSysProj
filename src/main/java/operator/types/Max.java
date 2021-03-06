package operator.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import supervisor.Position;

import java.util.List;
import java.util.Map;

/**
 * Created by higla on 06/03/18.
 *
 * This class represents the "Max" operator
 *
 */
public class Max extends OperatorType {

    /**
     * @param size The number of items that will be summed
     * @param slide The slide of the sum
     */
    public Max(int size, int slide, @Nullable Position source, @NotNull List<List<Position>> destination,
               List<Position> exactPosition) {
        super(destination, size, slide, source, exactPosition);
    }

    protected float operationType(List<Float> streamDatas){
        return streamDatas.stream().reduce(Float::max).orElse((float)0.0);
    }


    @Override
    public String toString() {
        return "Max{" +
                "size=" + size +
                ", slide=" + slide +
                '}';
    }

}
