package org.myeslib.core;


public interface CommandHandler<A extends AggregateRoot> {
	
//	<C extends Command, E extends Event> List<E> handle(C command);

}


//interface HandleCommand<C extends Command >{
//	List<? extends Event> handle(C command);
//}
//
//class Handler implements HandleCommand<CreateInventoryItem>, HandleCommand<IncreaseInventory> {
//	@Override
//	public List<? extends Event> handle(CreateInventoryItem command) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//	
//}