package org.andresoviedo.app.util;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CharChokkaJsonVO {
    public String reqText;
    public String chemicalString;
    public String chemicalFormula;
    public String wikiUrl;
    public String youTubeUrl;
    @JsonProperty("3dModelUrl")
    public String _3dModelUrl;
}
