package example.cashcard;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

import java.security.Principal;

@RestController
@RequestMapping("/cashcards")
class CashCardController {
    private final CashcardRepository cashcardRepository;

    private CashCardController(CashcardRepository cashcardRepository) {
        this.cashcardRepository = cashcardRepository;
    }


    @GetMapping("/{requestedId}")
    private ResponseEntity<CashCard> findById(@PathVariable Long requestedId, Principal principal) {
        CashCard cashCard = cashcardRepository.findByIdAndOwner(requestedId,principal.getName());
        if(cashCard!=null){
            return ResponseEntity.ok(cashCard);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping
    private ResponseEntity<Iterable<CashCard>> findAll(Pageable pageable,Principal principal) {
        Page<CashCard> page = cashcardRepository.findByOwner(principal.getName(),
                PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        pageable.getSortOr//getSortOr provides default values for page size and sort
                                (Sort.by(Sort.Direction.DESC, "amount"))
                )
        );
        return ResponseEntity.ok(page.getContent());
    }


    @PostMapping
    private ResponseEntity<Void> createCashCard(@RequestBody CashCard newCashCardRequest
    ,UriComponentsBuilder ucb,Principal principal){
        CashCard cashCardWithOwner = new CashCard(null, newCashCardRequest.amount(),
                principal.getName());
        CashCard savedCashCard = cashcardRepository.save(cashCardWithOwner);
        URI locationOfNewCashCard = ucb
                .path("cashcards/{id}")
                .buildAndExpand(savedCashCard.id())
                .toUri();
        return ResponseEntity.created(locationOfNewCashCard).build();
    }

    @PutMapping("/{requestedId}")
    private ResponseEntity<Void> putCashCard(@PathVariable Long requestedId, @RequestBody CashCard cashCardUpdate
    ,Principal principal){
        CashCard cashCard = cashcardRepository.findByIdAndOwner(requestedId, principal.getName());
        if(cashCard!=null){
            CashCard updateCashCard = new CashCard(cashCard.id(),cashCardUpdate.amount(),principal.getName());
            cashcardRepository.save(updateCashCard);
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{requestedId}")
    private ResponseEntity<Void> deleteCashCard(@PathVariable Long requestedId, Principal principal){
        if(!cashcardRepository.existsByIdAndOwner(requestedId,principal.getName())){
            return ResponseEntity.notFound().build();
        }
        cashcardRepository.deleteById(requestedId);
        return ResponseEntity.noContent().build();
    }


}
