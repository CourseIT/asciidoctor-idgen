package ru.curs.asciidoctor_idgen;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api")
public class Controller {

    @PostMapping("/enrich")
    public ResponseEntity<Object> enrich(
            @RequestBody EnrichParams enrichParams
    ) throws IOException {
        if (enrichParams.getInput() == null)
            return new ResponseEntity<>("No input provided", HttpStatus.CONFLICT);
        if (enrichParams.getOutput() == null)
            return new ResponseEntity<>("No output provided", HttpStatus.CONFLICT);
        if (enrichParams.getJson() == null)
            return new ResponseEntity<>("No json provided", HttpStatus.CONFLICT);
        Main.enrich(enrichParams.getInput(), enrichParams.getOutput()
                , enrichParams.getJson(), enrichParams.getOhtml() != null);
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
