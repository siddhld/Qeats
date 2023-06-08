
package com.crio.qeats.repositories;

import com.crio.qeats.models.ItemEntity;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface ItemRepository extends MongoRepository<ItemEntity, String> {

    @Query("{'itemName': {$regex: ?0, $options: 'i'}}")
    List<ItemEntity> findItemsByItemName(String itemName);
}

