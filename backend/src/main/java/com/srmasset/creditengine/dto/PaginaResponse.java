package com.srmasset.creditengine.dto;

import java.util.List;

public record PaginaResponse<T>(
    List<T> content, int page, int size, long totalElements, int totalPages) {

  public static <T> PaginaResponse<T> de(List<T> content, int page, int size, long totalElements) {
    int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    return new PaginaResponse<>(content, page, size, totalElements, totalPages);
  }
}
