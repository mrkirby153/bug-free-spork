package com.mrkirby153.bfs.model.enhancers;

import com.mrkirby153.bfs.model.Constants;
import com.mrkirby153.bfs.model.Enhancer;
import com.mrkirby153.bfs.model.Model;
import com.mrkirby153.bfs.model.ModelQueryBuilder;
import com.mrkirby153.bfs.model.SoftDeletingModel;
import com.mrkirby153.bfs.query.event.QueryEvent;
import com.mrkirby153.bfs.query.event.QueryEvent.Type;
import com.mrkirby153.bfs.query.event.QueryEventListener;

/**
 * Enhancer for soft deleting models
 */
public class SoftDeleteEnhancer implements Enhancer {

    @Override
    public void enhance(ModelQueryBuilder<? extends Model> builder) {
        builder.registerListener(Type.PRE_DELETE, new SoftDeleteQueryListener());
    }

    @Override
    public String name() {
        return Constants.ENHANCER_SOFT_DELETE;
    }

    public class SoftDeleteQueryListener implements QueryEventListener {

        @Override
        public void onEvent(QueryEvent event) {
            ModelQueryBuilder mqb = (ModelQueryBuilder) event.getQueryBuilder();
            SoftDeletingModel m = (SoftDeletingModel) mqb.getModel();
            m.touchDeletedAt();
            mqb.setModel(m);
            mqb.save();
            event.setCanceled(true);
        }
    }
}
