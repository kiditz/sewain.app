package app.sewain.catalogs;

import app.sewain.shared.PageQuery;
import app.sewain.shared.PageResponse;

import java.util.List;
import java.util.UUID;

interface AttributeQueryService {

    List<AttributeResponse> findAll();
    List<AttributeOptionResponse> findOptions();
    PageResponse<AttributeResponse> findAll(PageQuery q);
    AttributeResponse findById(UUID id);
}