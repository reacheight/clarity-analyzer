package skadistats.clarity.analyzer.replay;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import skadistats.clarity.model.EngineType;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.processor.entities.OnEntityCreated;
import skadistats.clarity.processor.entities.OnEntityDeleted;
import skadistats.clarity.processor.entities.OnEntityUpdated;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FXEntities {

    private final ObservableList<FxEntity> fxEntities = FXCollections.observableArrayList();

    public FXEntities(EngineType engineType) {
        for (int i = 0; i < (1 << engineType.getIndexBits()); i++) {
            fxEntities.add(new FxEntity());
        }
    }

    public ObservableList<FxEntity> getFxEntities() {
        return fxEntities;
    }

    @OnEntityCreated
    public void onEntityCreated(Entity entity) {
        Platform.runLater(() -> fxEntities.get(entity.getIndex()).setEntity(entity));
    }

    @OnEntityDeleted
    public void onEntityDeleted(Entity entity) {
        Platform.runLater(() -> fxEntities.get(entity.getIndex()).setEntity(null));
    }

    @OnEntityUpdated
    public void onEntityUpdated(Entity entity, FieldPath[] fieldPaths, int num) {
        Platform.runLater(() -> fxEntities.get(entity.getIndex()).update(fieldPaths, num));
    }

    public class FxEntity {

        private final ObjectProperty<Entity> entity;
        private final StringBinding index;
        private final StringBinding name;
        private final ObservableList<FxProperty> fxProperties = FXCollections.observableArrayList();

        FxEntity() {
            this.entity = new SimpleObjectProperty<>(this, "entity");
            this.entity.addListener((obs, o, n) -> entityChanged());
            this.index = Bindings.createStringBinding(
                    () -> String.valueOf(fxEntities.indexOf(this))
            );
            this.name = Bindings.createStringBinding(
                    () -> getEntity() != null ? getEntity().getDtClass().getDtName() : "",
                    entity
            );
        }

        public void entityChanged() {
            List<FieldPath> fps = getEntity() != null ? getEntity().getDtClass().collectFieldPaths(getEntity().getState()) : Collections.emptyList();

            int n = fps.size();
            int c = fxProperties.size();

            for (int i = 0; i < Math.min(n, c); i++) {
                fxProperties.get(i).setFieldPath(fps.get(i));
            }
            if (c < n) {
                List<FxProperty> toAdd = new ArrayList<>();
                for (int i = c; i < n; i++) {
                    FxProperty newProp = new FxProperty();
                    newProp.setFieldPath(fps.get(i));
                    toAdd.add(newProp);
                }
                fxProperties.addAll(toAdd);
            } else if (c > n) {
                fxProperties.removeAll(fxProperties.subList(n, c));
            }
        }

        public void update(FieldPath[] fpChanged, int num) {
            for (int i = 0; i < num; i++) {
                for (FxProperty fxProperty : fxProperties) {
                    if (fpChanged[i].equals(fxProperty.getFieldPath())) {
                        fxProperty.markInvalid();
                        break;
                    }
                }
            }
        }

        public Entity getEntity() {
            return entity.get();
        }

        public ObjectProperty<Entity> entityProperty() {
            return entity;
        }

        public void setEntity(Entity entity) {
            this.entity.set(entity);
        }

        public ObservableList<FxProperty> getFxProperties() {
            return fxProperties;
        }

        public String getIndex() {
            return index.get();
        }

        public StringBinding indexProperty() {
            return index;
        }

        public String getName() {
            return name.get();
        }

        public StringBinding nameProperty() {
            return name;
        }


        public class FxProperty {

            private final ObjectProperty<FieldPath> fieldPath;
            private final ObjectBinding<Object> state;
            private final StringBinding index;
            private final StringBinding name;
            private final StringBinding value;

            FxProperty() {
                this.fieldPath = new SimpleObjectProperty<>(this, "fieldPath");
                this.state = Bindings.createObjectBinding(
                        () -> getEntity() != null && getFieldPath() != null ? getEntity().getPropertyForFieldPath(getFieldPath()) : null,
                        entity, fieldPath
                );
                this.index = Bindings.createStringBinding(
                        () -> getFieldPath() != null ? getFieldPath().toString() : null,
                        fieldPath
                );
                this.name = Bindings.createStringBinding(
                        () -> getEntity() != null && getFieldPath() != null ? getEntity().getDtClass().getNameForFieldPath(getFieldPath()) : null,
                        entity, fieldPath
                );
                this.value = Bindings.createStringBinding(
                        () -> getState() != null ? getState().toString() : null,
                        state
                );
            }

            private void markInvalid() {
                this.state.invalidate();
            }

            public FieldPath getFieldPath() {
                return fieldPath.get();
            }

            public ObjectProperty<FieldPath> fieldPathProperty() {
                return fieldPath;
            }

            public void setFieldPath(FieldPath fieldPath) {
                this.fieldPath.set(fieldPath);
            }

            public Object getState() {
                return state.get();
            }

            public ObjectBinding<Object> stateProperty() {
                return state;
            }

            public String getIndex() {
                return index.get();
            }

            public StringBinding indexProperty() {
                return index;
            }

            public String getName() {
                return name.get();
            }

            public StringBinding nameProperty() {
                return name;
            }

            public String getValue() {
                return value.get();
            }

            public StringBinding valueProperty() {
                return value;
            }
        }

    }

}
