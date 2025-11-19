package com.sri.service;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.sri.dto.DocumentoDTO;

@Service
public class SolrSearchService {

    @SuppressWarnings("deprecation")
    private final HttpSolrClient solrClient;

    @SuppressWarnings("deprecation")
    public SolrSearchService(HttpSolrClient solrClient) {
        this.solrClient = solrClient;
    }

    public List<DocumentoDTO> search(String queryString) throws SolrServerException, IOException {
        SolrQuery query = new SolrQuery();
        // 1. Cogemos las palabras limpias

        String[] palabras = queryString.split("\\s+");

        List<String> palabrasLimpias = Arrays.stream(palabras)

                .map(palabra -> palabra.replaceAll("[^a-zA-Z0-9]", "")) // Quitamos caracteres especiales

                .filter(palabra -> !palabra.isEmpty()) // Quitamos palabras vacias

                .collect(Collectors.toList());

        // 2. Construir la consulta OR

        String joinedQuery = palabrasLimpias.stream()

                .map(palabra -> "body:" + palabra)
                .collect(Collectors.joining(" OR "));

        System.out.println("Ejecutando Query ID " + queryString + ": " + joinedQuery);
        query.setQuery("body:" + queryString);
        query.setFields("id", "body");
        query.setRows(Integer.MAX_VALUE); // Limitar a 50 resultados
        QueryResponse response = solrClient.query(query);
        SolrDocumentList documents = response.getResults();

        List<DocumentoDTO> resultado = new ArrayList<>();
        for (SolrDocument doc : documents) {
            Object tituloField = doc.getFieldValue("id");
            Object descripcionField = doc.getFieldValue("body");

            ArrayList<String> tituloList = toStringList(tituloField);
            ArrayList<String> descripcionList = toStringList(descripcionField);

            DocumentoDTO dto = new DocumentoDTO(
                    (String) doc.getFieldValue("id"),
                    tituloList,
                    descripcionList);
            resultado.add(dto);
        }

        return resultado;
    }

    // Helper to convert a Solr field value (String or Collection) into
    // ArrayList<String>
    private ArrayList<String> toStringList(Object fieldValue) {
        if (fieldValue == null) {
            return null;
        }
        if (fieldValue instanceof List) {
            List<?> raw = (List<?>) fieldValue;
            ArrayList<String> out = new ArrayList<>();
            for (Object o : raw) {
                if (o != null)
                    out.add(o.toString());
            }
            return out;
        }
        // single value
        return new ArrayList<>(Collections.singletonList(fieldValue.toString()));
    }
}