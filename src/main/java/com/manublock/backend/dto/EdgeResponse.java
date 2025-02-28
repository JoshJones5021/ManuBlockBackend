package com.manublock.backend.dto;

import com.manublock.backend.models.Edges;

public class EdgeResponse {
    private Long id;
    private NodeReference source;
    private NodeReference target;
    private Boolean animated;
    private String strokeColor;
    private Integer strokeWidth;

    // ✅ Constructor that accepts an Edges object
    public EdgeResponse(Edges edge) {
        this.id = edge.getId();
        this.source = edge.getSource() != null ? new NodeReference(edge.getSource().getId(), edge.getSource().getName()) : null;
        this.target = edge.getTarget() != null ? new NodeReference(edge.getTarget().getId(), edge.getTarget().getName()) : null;
        this.animated = edge.getAnimated();
        this.strokeColor = edge.getStrokeColor();
        this.strokeWidth = edge.getStrokeWidth();
    }

    public static class NodeReference {
        private Long id;
        private String name;

        public NodeReference(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() { return id; }
        public String getName() { return name; }
    }

    public Long getId() { return id; }
    public NodeReference getSource() { return source; }
    public NodeReference getTarget() { return target; }
    public Boolean getAnimated() { return animated; }
    public String getStrokeColor() { return strokeColor; }
    public Integer getStrokeWidth() { return strokeWidth; }
}
