package com.khamphaviet.restaurant.walkin;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/staff/walk-ins")
public class WalkInController {
    private final WalkInService service;
    public WalkInController(WalkInService service){this.service=service;}
    @GetMapping public List<WalkInDtos.VisitResponse> list(){return service.list();}
    @GetMapping("/metrics") public WalkInDtos.MetricsResponse metrics(){return service.metrics();}
    @PostMapping("/demo-scenario") public WalkInService.DemoScenarioResponse demo(Authentication auth){
        return service.createDemoScenario(auth.getName());
    }
    @GetMapping("/{id}") public WalkInDtos.VisitResponse get(@PathVariable Long id){return service.get(id);}
    @PostMapping public WalkInDtos.VisitResponse create(@Valid @RequestBody WalkInDtos.CreateRequest request,Authentication auth){return service.create(request,auth.getName());}
    @PatchMapping("/{id}/quote") public WalkInDtos.VisitResponse quote(@PathVariable Long id,@Valid @RequestBody WalkInDtos.ActionRequest request,Authentication auth){
        return service.reviseQuote(id,request.quotedWaitMinutes()==null?0:request.quotedWaitMinutes(),request.note(),auth.getName());}
    @PostMapping("/{id}/offer") public WalkInDtos.VisitResponse offer(@PathVariable Long id,@Valid @RequestBody WalkInDtos.OfferRequest request,Authentication auth){return service.offer(id,request,auth.getName());}
    @PostMapping("/{id}/call-again") public WalkInDtos.VisitResponse callAgain(@PathVariable Long id,@RequestBody(required=false) WalkInDtos.ActionRequest request,Authentication auth){return service.callAgain(id,request==null?null:request.note(),auth.getName());}
    @PostMapping("/{id}/seat") public WalkInDtos.VisitResponse seat(@PathVariable Long id,@RequestBody(required=false) WalkInDtos.ActionRequest request,Authentication auth){return service.seat(id,request==null?null:request.note(),auth.getName());}
    @PostMapping("/{id}/dining") public WalkInDtos.VisitResponse dining(@PathVariable Long id,@RequestBody(required=false) WalkInDtos.ActionRequest request,Authentication auth){return service.dining(id,request==null?null:request.note(),auth.getName());}
    @PostMapping("/{id}/payment") public WalkInDtos.VisitResponse payment(@PathVariable Long id,@RequestBody(required=false) WalkInDtos.ActionRequest request,Authentication auth){return service.requestPayment(id,request==null?null:request.note(),auth.getName());}
    @PostMapping("/{id}/finish") public WalkInDtos.VisitResponse finish(@PathVariable Long id,@RequestBody(required=false) WalkInDtos.ActionRequest request,Authentication auth){return service.finishService(id,request==null?null:request.note(),auth.getName());}
    @PostMapping("/{id}/cleaned") public WalkInDtos.VisitResponse cleaned(@PathVariable Long id,@RequestBody(required=false) WalkInDtos.ActionRequest request,Authentication auth){return service.cleaned(id,request==null?null:request.note(),auth.getName());}
    @PostMapping("/{id}/exit/{status}") public WalkInDtos.VisitResponse exit(@PathVariable Long id,@PathVariable WalkInStatus status,@RequestBody(required=false) WalkInDtos.ActionRequest request,Authentication auth){return service.exit(id,status,request==null?null:request.note(),auth.getName());}
    @PostMapping("/{id}/requeue") public WalkInDtos.VisitResponse requeue(@PathVariable Long id,@RequestBody(required=false) WalkInDtos.ActionRequest request,Authentication auth){return service.requeue(id,request==null?null:request.quotedWaitMinutes(),request==null?null:request.note(),auth.getName());}
}
