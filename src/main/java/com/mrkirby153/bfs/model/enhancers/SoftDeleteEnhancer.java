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

    private static final SoftDeleteQueryListener sdql = new SoftDeleteQueryListener();

    @Override
    public void enhance(ModelQueryBuilder<? extends Model> builder) {
        builder.registerListener(Type.PRE_DELETE, sdql);
    }

    @Override
    public void onQuery(ModelQueryBuilder<? extends Model> builder) {
        SoftDeletingModel.getDeletedAtCols(builder.getModelClass()).forEach(builder::whereNull);
    }

    @Override
    public void onUpdate(Model model, ModelQueryBuilder<? extends Model> builder) {
        SoftDeletingModel.getDeletedAtCols(builder.getModelClass()).forEach(builder::whereNull);
    }

    @Override
    public String name() {
        return Constants.ENHANCER_SOFT_DELETE;
    }

    private static class SoftDeleteQueryListener implements QueryEventListener {

        @Override
        public void onEvent(QueryEvent event) {
            ModelQueryBuilder<? extends Model> mqb = (ModelQueryBuilder<? extends Model>) event
                .getQueryBuilder();
            if (mqb.getModel() instanceof SoftDeletingModel) {
                SoftDeletingModel m = (SoftDeletingModel) mqb.getModel();
                // If we're force deleting models
                if (m.isForced()) {
                    return;
                }
                m.touchDeletedAt();
                m.setExists(false);
                mqb.save();
                event.setCanceled(true);
            }
        }
    }
}
