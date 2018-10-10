# opennxmlparser

Parser for NXML biomedical article files, produced by the National Library of Medicine (NLM).
This repo extends the NLM parser provided for TREC CDS.

## Example usage

import gov.nih.nlm.iti.driver.Driver;

import gov.nih.nlm.iti.text.model.Document;

...

Document document = Driver.getDocInfo(s);

System.out.println(document.getFullText());
