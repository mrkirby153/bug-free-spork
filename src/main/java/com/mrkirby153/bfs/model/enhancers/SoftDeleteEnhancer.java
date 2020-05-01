package com.mrkirby153.bfs.model.enhancers;

import com.mrkirby153.bfs.Pair;
import com.mrkirby153.bfs.model.Constants;
import com.mrkirby153.bfs.model.Enhancer;
import com.mrkirby153.bfs.model.Model;
import com.mrkirby153.bfs.model.ModelQueryBuilder;
import com.mrkirby153.bfs.model.SoftDeletingModel;
import com.mrkirby153.bfs.query.event.QueryEvent;
import com.mrkirby153.bfs.query.event.QueryEvent.Type;
import com.mrkirby153.bfs.query.event.QueryEventListener;
import lombok.extern.slf4j.Slf4j;

import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Enhancer for soft deleting models
 */
@Slf4j
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
    public String name() {
        return Constants.ENHANCER_SOFT_DELETE;
    }

    private static class SoftDeleteQueryListener implements QueryEventListener {

        @Override
        public void onEvent(QueryEvent event) {
            ModelQueryBuilder<? extends Model> mqb = (ModelQueryBuilder<? extends Model>) event
                .getQueryBuilder();
            if (SoftDeletingModel.class.isAssignableFrom(mqb.getModelClass())) {
                log.trace("Soft deleting model {}", mqb.getModel());
                if (mqb.getModel() != null) {
                    // There is a model that we're modifying
                    SoftDeletingModel m = (SoftDeletingModel) mqb.getModel();
                    if (m.isForced()) {
                        return;
                    }
                    m.touchDeletedAt();
                    mqb.save();
                } else {
                    // There is no model bound
                    Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
                    List<Pair<String, Object>> cols = SoftDeletingModel
                        .getDeletedAtCols(mqb.getModelClass()).stream()
                        .map(col -> new Pair<String, Object>(col, currentTimestamp)).collect(Collectors.toList());
                    SoftDeletingModel.getDeletedAtCols(mqb.getModelClass()).forEach(mqb::whereNull);
                    mqb.update(cols);
                }
                event.setCanceled(true);
            }
        }
    }
}
